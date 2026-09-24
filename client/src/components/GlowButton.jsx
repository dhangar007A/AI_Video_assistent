import { motion } from 'framer-motion';
import './GlowButton.css';

export default function GlowButton({
  children,
  onClick,
  disabled = false,
  loading = false,
  variant = 'primary',
  size = 'md',
  icon,
  className = '',
  ...props
}) {
  return (
    <motion.button
      className={`glow-btn glow-btn--${variant} glow-btn--${size} ${className}`}
      onClick={onClick}
      disabled={disabled || loading}
      whileHover={{ scale: disabled ? 1 : 1.02 }}
      whileTap={{ scale: disabled ? 1 : 0.98 }}
      {...props}
    >
      {loading && <span className="glow-btn__spinner" />}
      {!loading && icon && <span className="glow-btn__icon">{icon}</span>}
      <span className="glow-btn__text">{children}</span>
      <span className="glow-btn__glow" />
    </motion.button>
  );
}
