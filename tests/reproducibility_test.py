import importlib.util
import io
from pathlib import Path
import tempfile
import unittest
import zipfile

spec = importlib.util.spec_from_file_location('apk_payload', Path(__file__).resolve().parents[1] / 'scripts' / 'apk-payload.py')
apk_payload = importlib.util.module_from_spec(spec)
spec.loader.exec_module(apk_payload)
identity_spec = importlib.util.spec_from_file_location('build_identity', Path(__file__).resolve().parents[1] / 'scripts' / 'build-identity.py')
build_identity = importlib.util.module_from_spec(identity_spec)
identity_spec.loader.exec_module(build_identity)


class ArtifactIdentityTests(unittest.TestCase):
    def test_mode_variant_and_unsigned_identity(self):
        gradle = 'versionName "2.7.1"\nversionCode 271\n'
        self.assertEqual(build_identity.artifact_identity({'version': '2.7.1'}, gradle, 'github', 'debug'), 'muajjin-2.7.1-271-github-debug.apk')
        self.assertEqual(build_identity.artifact_identity({'version': '2.7.1'}, gradle, 'nongithub', 'release'), 'muajjin-2.7.1-271-nongithub-release-unsigned.apk')

    def test_version_mismatch_fails_instead_of_mislabeling(self):
        with self.assertRaisesRegex(ValueError, 'matching'):
            build_identity.artifact_identity({'version': '2.7.2'}, 'versionName "2.7.1"\nversionCode 271\n', 'github', 'release')

    def test_ambiguous_native_version_fails(self):
        with self.assertRaisesRegex(ValueError, 'one literal'):
            build_identity.artifact_identity({'version': '2.7.1'}, 'versionName "2.7.1"\nversionCode 271\nversionCode 272\n', 'github', 'debug')


class PayloadTests(unittest.TestCase):
    def archive(self, entries, compression=zipfile.ZIP_STORED):
        data = io.BytesIO()
        with zipfile.ZipFile(data, 'w', compression=compression) as archive:
            for name, content in entries:
                info = zipfile.ZipInfo(name)
                # Preserve hostile names on Windows: ZipInfo normally normalizes OS separators.
                info.filename = name
                info.compress_type = compression
                archive.writestr(info, content)
        return data.getvalue()

    def manifest(self, data, v1=False):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'test.apk'
            path.write_bytes(data)
            return apk_payload.payload_manifest(path, verified_v1=v1)

    def test_zip_order_and_compression_do_not_change_payload(self):
        entries = [('AndroidManifest.xml', b'manifest'), ('classes.dex', b'code')]
        self.assertEqual(self.manifest(self.archive(entries)), self.manifest(self.archive(entries[::-1], zipfile.ZIP_DEFLATED)))

    def test_dependency_metadata_is_not_excluded(self):
        first = self.archive([('classes.dex', b'code'), ('META-INF/library.version', b'1')])
        second = self.archive([('classes.dex', b'code'), ('META-INF/library.version', b'2')])
        self.assertNotEqual(self.manifest(first, True), self.manifest(second))

    def test_only_verified_v1_signing_namespace_is_excluded(self):
        entries = [('classes.dex', b'code'), ('META-INF/MANIFEST.MF', b'jar'), ('META-INF/RELEASE.SF', b'sf'), ('META-INF/RELEASE.RSA', b'block'), ('META-INF/nested/RELEASE.SF', b'payload')]
        manifest = self.manifest(self.archive(entries), True)
        self.assertEqual(set(manifest), {'classes.dex', 'META-INF/nested/RELEASE.SF'})
        self.assertEqual(len(self.manifest(self.archive(entries))), 5)

    def test_duplicate_entry_names_fail_closed(self):
        with self.assertWarns(UserWarning):
            data = self.archive([('classes.dex', b'first'), ('classes.dex', b'second')])
        with self.assertRaisesRegex(ValueError, 'Duplicate'):
            self.manifest(data)

    def test_unsafe_paths_fail_closed_without_extracting(self):
        for name in ('../classes.dex', '/classes.dex', 'assets/../classes.dex', 'assets\\classes.dex'):
            with self.subTest(name=name), self.assertRaisesRegex(ValueError, 'Unsafe'):
                self.manifest(self.archive([(name, b'data')]))

    def test_directory_entries_remain_part_of_payload(self):
        self.assertNotEqual(self.manifest(self.archive([('assets/', b''), ('assets/a', b'a')])), self.manifest(self.archive([('assets/a', b'a')])))

    def test_certificate_and_scheme_parser_require_verified_output(self):
        digest = 'a' * 64
        output = 'Verifies\nVerified using v1 scheme (JAR signing): true\nSigner #1 certificate SHA-256 digest: ' + digest
        self.assertEqual(apk_payload.signature_details(output, digest.upper()), True)
        with self.assertRaisesRegex(ValueError, 'certificate'):
            apk_payload.signature_details(output, 'b' * 64)
        with self.assertRaisesRegex(ValueError, 'signer'):
            apk_payload.signature_details(output + '\nSigner #2 certificate SHA-256 digest: ' + digest, digest)

    def test_unsigned_candidate_cannot_contain_v1_signature_files(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'candidate.apk'
            path.write_bytes(self.archive([('classes.dex', b'code'), ('META-INF/RELEASE.SF', b'signature')]))
            with self.assertRaisesRegex(ValueError, 'unsigned'):
                apk_payload.require_unsigned(path)

    def test_unsigned_candidate_cannot_contain_apk_signing_block(self):
        data = bytearray(self.archive([('classes.dex', b'code')]))
        directory_offset = data.index(b'PK\x01\x02')
        # A signing block is outside ZIP entries immediately before the directory.
        block = (24).to_bytes(8, 'little') + (24).to_bytes(8, 'little') + b'APK Sig Block 42'
        data[directory_offset:directory_offset] = block
        eocd = data.rindex(b'PK\x05\x06')
        data[eocd + 16:eocd + 20] = (directory_offset + len(block)).to_bytes(4, 'little')
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'candidate.apk'
            path.write_bytes(data)
            with self.assertRaisesRegex(ValueError, 'unsigned'):
                apk_payload.require_unsigned(path)


if __name__ == '__main__':
    unittest.main()
