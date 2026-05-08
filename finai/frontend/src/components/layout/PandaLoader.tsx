import { motion, AnimatePresence } from 'framer-motion'
import { useAppStore } from '@/store/useAppStore'

export function PandaLoader() {
  const { pendingOps, loaderMessage } = useAppStore()
  const visible = pendingOps > 0

  return (
    <AnimatePresence>
      {visible && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          style={{
            position: 'fixed', inset: 0, zIndex: 9999,
            display: 'flex', flexDirection: 'column',
            alignItems: 'center', justifyContent: 'center',
            backdropFilter: 'blur(8px)',
            background: 'rgba(7,8,10,0.7)',
          }}
        >
          <motion.div
            animate={{ y: [0, -12, 0] }}
            transition={{ repeat: Infinity, duration: 0.8, ease: 'easeInOut' }}
          >
            <svg viewBox="0 0 100 100" width={90} height={90}>
              <ellipse cx="22" cy="22" rx="14" ry="14" fill="#1a1a1a"/>
              <ellipse cx="78" cy="22" rx="14" ry="14" fill="#1a1a1a"/>
              <ellipse cx="50" cy="54" rx="38" ry="36" fill="#f0f0f0"/>
              <ellipse cx="35" cy="46" rx="12" ry="11" fill="#1a1a1a"/>
              <ellipse cx="65" cy="46" rx="12" ry="11" fill="#1a1a1a"/>
              <text x="35" y="51" textAnchor="middle" fontSize="13" fontWeight="900" fill="#e8f542">€</text>
              <text x="65" y="51" textAnchor="middle" fontSize="13" fontWeight="900" fill="#e8f542">€</text>
              <ellipse cx="50" cy="65" rx="9" ry="6" fill="#1a1a1a"/>
              <path d="M42 73 Q50 80 58 73" stroke="#1a1a1a" strokeWidth="2.5" fill="none" strokeLinecap="round"/>
              <ellipse cx="28" cy="68" rx="7" ry="5" fill="#f9a8d4" opacity={0.6}/>
              <ellipse cx="72" cy="68" rx="7" ry="5" fill="#f9a8d4" opacity={0.6}/>
            </svg>
          </motion.div>

          <p style={{ marginTop: 16, color: 'var(--text)', fontFamily: 'Syne', fontSize: 14, fontWeight: 600 }}>
            {loaderMessage}
          </p>

          <div style={{
            marginTop: 12, width: 200, height: 4, background: 'var(--s3)',
            borderRadius: 2, overflow: 'hidden',
          }}>
            <motion.div
              animate={{ x: ['-100%', '200%'] }}
              transition={{ repeat: Infinity, duration: 1.2, ease: 'easeInOut' }}
              style={{ height: '100%', width: '50%', background: 'var(--acc)', borderRadius: 2 }}
            />
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  )
}
