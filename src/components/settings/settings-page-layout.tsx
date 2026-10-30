import { BottomNav } from '@/components/bottom-nav';
import { Button } from '@/components/ui/button';
import { useTranslation } from '@/contexts/translation-context';
import { ArrowLeft } from 'lucide-react';
import type { FC, ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';

interface SettingsPageLayoutProps {
  title: string;
  children: ReactNode;
  contentClassName?: string;
  showBackButton?: boolean;
}

export const SettingsPageLayout: FC<SettingsPageLayoutProps> = ({
  title,
  children,
  contentClassName = 'mx-auto max-w-md space-y-6 px-5 py-6',
  showBackButton = true,
}) => {
  const navigate = useNavigate();
  const { t } = useTranslation();

  return (
    <div className="min-h-screen bg-background pb-[var(--app-bottom-space)]">
      <div className={contentClassName}>
        <div className="space-y-4">
          {showBackButton ? (
            <div className="flex items-center gap-2">
              <Button
                variant="ghost"
                size="icon"
                aria-label={t('common.back')}
                onClick={() => navigate(-1)}
                className="size-10 shrink-0 rounded-full">
                <ArrowLeft className="h-5 w-5 rtl:rotate-180" />
              </Button>
              <h1 className="min-w-0 flex-1 break-words text-center text-2xl font-bold tracking-tight">
                {title}
              </h1>
              <div className="size-10 shrink-0" aria-hidden="true" />
            </div>
          ) : (
            <div className="space-y-1">
              <h1 className="text-3xl font-bold tracking-tight">{title}</h1>
            </div>
          )}
        </div>
        {children}
      </div>
      <BottomNav />
    </div>
  );
};
