import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { cn } from '@/lib/utils';
import type { HTMLInputTypeAttribute } from 'react';

interface LabeledInputFieldProps {
  id: string;
  label: string;
  value: string | number;
  onChange: (value: string) => void;
  placeholder?: string;
  type?: HTMLInputTypeAttribute;
  step?: string | number;
  min?: number;
  max?: number;
  required?: boolean;
  containerClassName?: string;
  inputClassName?: string;
}

export function LabeledInputField({
  id,
  label,
  value,
  onChange,
  placeholder,
  type = 'text',
  step,
  min,
  max,
  required,
  containerClassName = 'space-y-1',
  inputClassName = 'bg-card',
}: LabeledInputFieldProps) {
  return (
    <div className={cn(containerClassName)}>
      <Label htmlFor={id}>{label}</Label>
      <Input
        id={id}
        type={type}
        dir={type === 'number' && value !== '' ? 'ltr' : undefined}
        step={step}
        min={min}
        max={max}
        required={required}
        placeholder={placeholder}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className={cn(type === 'number' && 'tabular-nums', inputClassName)}
      />
    </div>
  );
}
