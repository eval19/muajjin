import {
  Sheet,
  SheetContent,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet';
import { useTranslation } from '@/contexts/translation-context';
import { cn } from '@/lib/utils';
import { BookCheck, CalendarDays, Compass, Home, LayoutGrid, Settings } from 'lucide-react';
import { useCallback, useState } from 'react';
import type { ComponentType } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';

interface NavItemData {
  labelKey: string;
  to: string;
  icon: ComponentType<{ className?: string }>;
  matches: (pathname: string) => boolean;
}

const NAV_ITEMS: NavItemData[] = [
  {
    labelKey: 'navigation.home',
    to: '/',
    icon: Home,
    matches: (pathname) => pathname === '/',
  },
  {
    labelKey: 'navigation.calendar',
    to: '/upcoming-prayer-times',
    icon: CalendarDays,
    matches: (pathname) => pathname.startsWith('/upcoming-prayer-times'),
  },
  {
    labelKey: 'navigation.compass',
    to: '/qibla',
    icon: Compass,
    matches: (pathname) => pathname.startsWith('/qibla'),
  },
  {
    labelKey: 'navigation.qazaTracker',
    to: '/qaza',
    icon: BookCheck,
    matches: (pathname) => pathname.startsWith('/qaza'),
  },
];

export function BottomNav() {
  const { pathname } = useLocation();
  const { t, direction } = useTranslation();
  const navigate = useNavigate();
  const [sheetOpen, setSheetOpen] = useState(false);
  const closeSheet = useCallback(() => setSheetOpen(false), []);

  const handleSettingsClick = useCallback(() => {
    if (pathname === '/settings') return;
    if (pathname.startsWith('/settings/')) navigate(-1);
    else navigate('/settings');
  }, [pathname, navigate]);

  return (
    <Sheet open={sheetOpen} onOpenChange={setSheetOpen}>
      <SheetContent
        side={direction === 'rtl' ? 'right' : 'left'}
        dir={direction}
        closeLabel={t('common.close')}
        aria-describedby={undefined}
        className="w-72 p-0">
        <header className="border-b border-border px-5 py-5">
          <div className="flex items-center gap-3 pe-12">
            <img src="/icon.png" alt="" className="h-10 w-10 rounded-xl" />
            <SheetTitle className="text-lg font-bold tracking-tight">
              {t('common.appName')}
            </SheetTitle>
          </div>
        </header>
        <nav aria-label={t('navigation.menu')} className="flex flex-col gap-2 px-3 py-4">
          {NAV_ITEMS.map((item) => {
            const Icon = item.icon;
            const active = item.matches(pathname);
            return (
              <Link
                key={item.to}
                to={item.to}
                onClick={closeSheet}
                aria-current={active ? 'page' : undefined}
                className={cn(
                  'flex items-center gap-3 rounded-xl border bg-card p-3 text-card-foreground shadow transition-colors hover:bg-accent/50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
                  active && 'border-primary/30 bg-primary/5',
                )}>
                <Icon className="h-5 w-5" />
                <span className="flex-1 font-semibold">{t(item.labelKey)}</span>
              </Link>
            );
          })}
        </nav>
      </SheetContent>
      <div className="fixed inset-x-0 bottom-0 z-40 border-t border-border bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/80">
        <div className="flex items-center justify-between px-5 pb-[calc(env(safe-area-inset-bottom,0px)+0.5rem)] pt-2">
          <SheetTrigger asChild>
            <button
              type="button"
              className="flex size-11 items-center justify-center rounded-full text-muted-foreground transition-colors hover:bg-accent hover:text-accent-foreground"
              aria-label={t('navigation.menu')}>
              <LayoutGrid className="h-5 w-5" />
            </button>
          </SheetTrigger>
          <button
            type="button"
            onClick={() => navigate('/')}
            className="min-h-11 cursor-pointer text-lg font-bold tracking-tight">
            {t('common.appName')}
          </button>
          <button
            type="button"
            onClick={handleSettingsClick}
            className="flex size-11 items-center justify-center rounded-full text-muted-foreground transition-colors hover:bg-accent hover:text-accent-foreground"
            aria-label={t('navigation.settings')}>
            <Settings className="h-5 w-5" />
          </button>
        </div>
      </div>
    </Sheet>
  );
}
