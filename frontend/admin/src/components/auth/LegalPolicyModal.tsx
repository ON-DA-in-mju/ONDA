import { useEffect, useState } from 'react'
import { X } from 'lucide-react'
import '../../styles/legal.css' // auth/ → styles/

type Tab = 'terms' | 'privacy'

type LegalPolicyModalProps = {
  open: boolean
  onClose: () => void
  initialTab?: Tab
}

const termsSections = [
  {
    title: '제1조 (목적)',
    body: '본 약관은 명지대학교 셔틀버스 통합 서비스 ON-DA(이하 "서비스")의 관리자 시스템 이용과 관련하여, 회사와 관리자 회원(이하 "회원") 간의 권리·의무 및 책임사항을 규정함을 목적으로 합니다.',
  },
  {
    title: '제2조 (정의)',
    body: '① "관리자 시스템"이란 운행 관제, 노선·차량·공지·제보 관리 등 서비스 운영을 위해 제공되는 웹 시스템을 말합니다.\n② "회원"이란 본 약관에 동의하고 관리자 계정을 부여받아 시스템을 이용하는 자를 말합니다.\n③ "계정"이란 회원 식별을 위해 부여된 이메일 및 인증 정보를 말합니다.',
  },
  {
    title: '제3조 (약관의 효력 및 변경)',
    body: '① 본 약관은 회원가입 화면 또는 서비스 내에 게시함으로써 효력이 발생합니다.\n② 회사는 관련 법령을 위배하지 않는 범위에서 약관을 개정할 수 있으며, 개정 시 적용일 및 개정 사유를 명시하여 사전에 공지합니다.\n③ 회원이 변경 약관에 동의하지 않을 경우 이용을 중단하고 탈퇴할 수 있습니다.',
  },
  {
    title: '제4조 (회원가입 및 계정 관리)',
    body: '① 회원가입은 이메일 인증 등 회사가 정한 절차를 완료한 후 승인될 수 있습니다.\n② 회원은 정확한 정보를 제공해야 하며, 허위 정보로 인한 불이익은 회원 본인에게 있습니다.\n③ 계정과 비밀번호의 관리 책임은 회원에게 있으며, 제3자 공유·양도를 금지합니다.\n④ 계정 도용 또는 부정 사용이 의심되는 경우 즉시 회사에 통지해야 합니다.',
  },
  {
    title: '제5조 (서비스의 제공 및 변경)',
    body: '회사는 대시보드, 운행·배차 관리, 실시간 관제, 공지·제보 처리, 사용자·시스템 관리 등의 기능을 제공합니다. 운영상 필요에 따라 기능을 추가·변경·중단할 수 있으며, 중요한 변경은 사전에 안내합니다.',
  },
  {
    title: '제6조 (회원의 의무)',
    body: '회원은 다음 행위를 해서는 안 됩니다.\n1. 타인의 개인정보 또는 계정을 무단으로 이용·수집하는 행위\n2. 서비스의 정상 운영을 방해하거나 시스템을 해킹·변조하는 행위\n3. 운행·학생 관련 민감 정보를 무단으로 외부에 유출하는 행위\n4. 법령 또는 본 약관에 위반되는 행위',
  },
  {
    title: '제7조 (서비스 이용 제한)',
    body: '회사가 본 약관 또는 운영정책을 위반한 회원에 대해 경고, 이용 제한, 계정 정지, 계약 해지 등의 조치를 할 수 있습니다.',
  },
  {
    title: '제8조 (책임의 제한)',
    body: '천재지변, 통신 장애, GPS 미수신 등 회사의 합리적 통제 범위를 벗어난 사유로 발생한 서비스 중단에 대해 회사는 법령이 허용하는 범위 내에서 책임을 제한합니다.',
  },
  {
    title: '제9조 (준거법 및 관할)',
    body: '본 약관은 대한민국 법령에 따르며, 분쟁 발생 시 민사소송법상 관할 법원에 제소합니다.',
  },
]

const privacySections = [
  {
    title: '1. 개인정보의 수집 항목',
    body: '회사는 관리자 회원가입 및 서비스 운영을 위해 다음 정보를 수집할 수 있습니다.\n- 필수: 이름, 이메일, 비밀번호, 역할(ADMIN/DRIVER/STUDENT)\n- 선택: 전화번호\n- 자동 수집: 접속 IP, 브라우저 정보, 로그인·이용 기록, 기기 정보',
  },
  {
    title: '2. 개인정보의 수집·이용 목적',
    body: '수집한 개인정보는 다음 목적으로만 이용합니다.\n1. 회원 식별 및 본인·이메일 인증\n2. 관리자 권한에 따른 서비스 제공 및 운영\n3. 보안, 부정이용 방지, 장애·로그 분석\n4. 공지·알림 발송 및 고객 문의 대응\n5. 관련 법령에 따른 보관·의무 이행',
  },
  {
    title: '3. 개인정보의 보유 및 이용 기간',
    body: '회원 탈퇴 시까지 보관·이용하며, 관련 법령에 따라 일정 기간 보관이 필요한 경우 해당 기간 동안 보관 후 파기합니다.\n- 계약 또는 청약철회 등에 관한 기록: 5년\n- 소비자 불만 또는 분쟁처리에 관한 기록: 3년\n- 웹사이트 방문 기록: 3개월\n시스템 운영 로그는 내부 정책에 따라 최대 1년 보관할 수 있습니다.',
  },
  {
    title: '4. 개인정보의 제3자 제공',
    body: '회사는 원칙적으로 회원의 개인정보를 외부에 제공하지 않습니다. 다만 법령에 근거한 요청이 있거나, 회원 동의를 받은 경우에 한해 제공할 수 있습니다.',
  },
  {
    title: '5. 개인정보 처리의 위탁',
    body: '원활한 서비스 제공을 위해 인증·인프라·이메일 발송 등 일부 업무를 외부에 위탁할 수 있으며, 위탁 시 관련 법령에 따라 안전하게 관리되도록 감독합니다.',
  },
  {
    title: '6. 개인정보의 파기',
    body: '보유 기간이 경과하거나 처리 목적이 달성된 개인정보는 지체 없이 파기합니다. 전자적 파일은 복구 불가능한 방법으로 삭제하고, 종이 문서는 분쇄 또는 소각합니다.',
  },
  {
    title: '7. 정보주체의 권리',
    body: '회원은 언제든지 개인정보 열람·정정·삭제·처리정지·동의 철회를 요청할 수 있습니다. 요청은 관리자 문의 메일(admin@mju.ac.kr)로 접수할 수 있으며, 회사는 관련 법령에 따라 지체 없이 처리합니다.',
  },
  {
    title: '8. 개인정보의 안전성 확보 조치',
    body: '회사는 개인정보 보호를 위해 접근권한 관리, 비밀번호 암호화, 접속기록 보관, 보안 프로그램 적용 등 합리적 보호조치를 시행합니다.',
  },
  {
    title: '9. 개인정보 보호책임자',
    body: '개인정보 관련 문의·불만·피해구제\n- 담당: ON-DA 관리자 시스템 운영팀\n- 이메일: admin@mju.ac.kr\n- 소속: 명지대학교 셔틀버스 통합 서비스',
  },
  {
    title: '10. 고지의 의무',
    body: '본 방침은 2026년 8월 7일부터 적용됩니다. 내용 추가·삭제·수정이 있을 경우 변경 사항과 시행일을 서비스 공지사항을 통해 안내합니다.',
  },
]

/** 이용약관·개인정보처리방침 모달 */
export function LegalPolicyModal({ open, onClose, initialTab = 'terms' }: LegalPolicyModalProps) {
  const [tab, setTab] = useState<Tab>(initialTab)

  useEffect(() => {
    if (open) setTab(initialTab)
  }, [open, initialTab])

  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', onKey)
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = ''
    }
  }, [open, onClose])

  if (!open) return null

  const sections = tab === 'terms' ? termsSections : privacySections

  return (
    <div className="legal-overlay" role="dialog" aria-modal="true" aria-labelledby="legal-title">
      <button type="button" className="legal-backdrop" aria-label="닫기" onClick={onClose} />
      <div className="legal-modal">
        <header className="legal-head">
          <div>
            <h2 id="legal-title">약관 및 개인정보 안내</h2>
            <p>ON-DA 관리자 시스템 이용을 위해 아래 내용을 확인해 주세요.</p>
          </div>
          <button type="button" className="legal-close" onClick={onClose} aria-label="닫기">
            <X size={18} />
          </button>
        </header>

        <div className="legal-tabs">
          <button
            type="button"
            className={tab === 'terms' ? 'active' : undefined}
            onClick={() => setTab('terms')}
          >
            이용약관
          </button>
          <button
            type="button"
            className={tab === 'privacy' ? 'active' : undefined}
            onClick={() => setTab('privacy')}
          >
            개인정보처리방침
          </button>
        </div>

        <div className="legal-body">
          <p className="legal-meta">
            {tab === 'terms' ? '시행일: 2026.08.07 · ON-DA 관리자 시스템 이용약관' : '시행일: 2026.08.07 · ON-DA 개인정보처리방침'}
          </p>
          {sections.map((s) => (
            <section key={s.title} className="legal-section">
              <h3>{s.title}</h3>
              <p>{s.body}</p>
            </section>
          ))}
        </div>

        <footer className="legal-foot">
          <button type="button" className="btn btn-primary" onClick={onClose}>
            확인
          </button>
        </footer>
      </div>
    </div>
  )
}
