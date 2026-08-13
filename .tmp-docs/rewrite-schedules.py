# -*- coding: utf-8 -*-
from pathlib import Path

path = Path(r"C:\Users\82108\Desktop\Bus_alpha\frontend\admin\src\pages\SchedulesPages.tsx")
text = path.read_text(encoding="utf-8")

marker_start = '  return (\n    <div className="page">\n      <div className="sched-grid">'
marker_end = "/** 기사 배정 전용 화면"
start = text.index(marker_start)
end = text.index(marker_end)

new_return = r'''  return (
    <div className="page">
      <div className="sched-grid">
        {/* 1행: 조회 ↔ 요약 (Y 높이 일치) */}
        <section className="card card-pad sched-card">
          <div className="card-head">
            <h3>운행 일정 조회</h3>
          </div>
          <div className="sched-filter-form">
            <div className="sched-filter-row sched-filter-row-top">
              <div className="sched-filter-field sched-filter-route">
                <span className="sched-filter-label">노선</span>
                <select
                  className="select"
                  value={routeFilter}
                  onChange={(e) => setRouteFilter(e.target.value)}
                >
                  <option value="">노선 전체</option>
                  {SCHEDULE_ROUTE_OPTIONS.map((name) => (
                    <option key={name} value={name}>
                      {name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="sched-filter-field sched-filter-days">
                <span className="sched-filter-label">요일</span>
                <div className="sched-day-group">
                  <button
                    type="button"
                    className={`sched-day-chip wide${weekday < 0 ? ' active' : ''}`}
                    onClick={() => setWeekday(-1)}
                  >
                    전체
                  </button>
                  {/* Figma 순서: 월~토 → 일 */}
                  {[1, 2, 3, 4, 5, 6, 0].map((i) => (
                    <button
                      key={WEEKDAY_LABELS[i]}
                      type="button"
                      className={`sched-day-chip${weekday === i ? ' active' : ''}`}
                      onClick={() => setWeekday(i)}
                    >
                      {WEEKDAY_LABELS[i]}
                    </button>
                  ))}
                </div>
              </div>
            </div>

            <div className="sched-filter-row sched-filter-row-bottom">
              <div className="sched-filter-field sched-filter-period">
                <span className="sched-filter-label">기간</span>
                <div className="sched-period-wrap">
                  <div className="sched-period-box">
                    <div className="sched-period-part">
                      <button
                        type="button"
                        onClick={() => setWeekPickerOpen((v) => !v)}
                        aria-expanded={weekPickerOpen}
                        aria-label="기간 시작일"
                      >
                        {formatDotDate(weekStart)}
                      </button>
                      <CalendarDays
                        size={16}
                        className="cal-icon"
                        onClick={() => setWeekPickerOpen((v) => !v)}
                      />
                    </div>
                    <span className="sched-period-sep">~</span>
                    <div className="sched-period-part">
                      <button
                        type="button"
                        onClick={() => setWeekPickerOpen((v) => !v)}
                        aria-label="기간 종료일"
                      >
                        {formatDotDate(weekEnd)}
                      </button>
                      <CalendarDays
                        size={16}
                        className="cal-icon"
                        onClick={() => setWeekPickerOpen((v) => !v)}
                      />
                    </div>
                  </div>
                  <WeekRangePicker
                    weekStart={weekStart}
                    open={weekPickerOpen}
                    onClose={() => setWeekPickerOpen(false)}
                    onPick={onPickWeekStart}
                  />
                </div>
              </div>

              <div className="sched-filter-actions">
                <button className="btn btn-primary" type="button" onClick={() => void load()}>
                  <Search size={15} style={{ marginRight: 6 }} />
                  조회하기
                </button>
                <button className="btn btn-outline" type="button" onClick={onReset}>
                  초기화
                </button>
              </div>
            </div>
          </div>
        </section>

        <section className="card card-pad sched-card">
          <div className="card-head">
            <h3>
              운행 일정 요약{' '}
              <span className="muted sched-summary-range">({weekLabel})</span>
            </h3>
            <div className="toolbar">
              <button className="btn btn-outline" type="button">
                <Download size={15} style={{ marginRight: 6 }} />
                엑셀 다운로드
              </button>
              <Link className="btn btn-primary" to="/schedules/bulk">
                <Plus size={15} style={{ marginRight: 4 }} />
                운행 일정 생성
              </Link>
            </div>
          </div>
          <div className="sched-summary-kpis">
            <div className="sched-summary-kpi">
              <div className="sched-summary-kpi-head">
                <div className="icon blue">
                  <CalendarDays size={23} strokeWidth={3} />
                </div>
                <div className="label">총 운행 횟수</div>
              </div>
              <div className="value">
                {weekSummary.tripCount.toLocaleString()}
                <em>회</em>
              </div>
              <div className="hint">일 평균 {weekSummary.avgTrips.toLocaleString()}회</div>
            </div>
            <div className="sched-summary-kpi">
              <div className="sched-summary-kpi-head">
                <div className="icon green">
                  <Clock size={23} strokeWidth={3} />
                </div>
                <div className="label">총 운행 시간</div>
              </div>
              <div className="value">
                {weekSummary.totalDur.h}
                <em>시간</em> {String(weekSummary.totalDur.m).padStart(2, '0')}
                <em>분</em>
              </div>
              <div className="hint">
                일 평균 {weekSummary.avgDur.h}시간 {String(weekSummary.avgDur.m).padStart(2, '0')}분
              </div>
            </div>
            <div className="sched-summary-kpi">
              <div className="sched-summary-kpi-head">
                <div className="icon orange">
                  <Bus size={23} strokeWidth={3} />
                </div>
                <div className="label">총 운행 차량</div>
              </div>
              <div className="value">
                {weekSummary.vehicleCount}
                <em>대</em>
              </div>
              <div className="hint">투입 차량 기준</div>
            </div>
            <div className="sched-summary-kpi">
              <div className="sched-summary-kpi-head">
                <div className="icon purple">
                  <Users size={23} strokeWidth={3} />
                </div>
                <div className="label">예상 탑승 인원</div>
              </div>
              <div className="value">
                {weekSummary.passengers.toLocaleString()}
                <em>명</em>
              </div>
              <div className="hint">일 평균 {weekSummary.avgPassengers.toLocaleString()}명</div>
            </div>
          </div>
        </section>

        {/* 2행: 목록 ↔ 공식 시간표 (Y 높이 일치) */}
        <section className="card card-pad sched-card">
          <div className="card-head">
            <h3>운행 일정 목록</h3>
            <div className="toolbar" style={{ gap: 8 }}>
              <Link className="btn btn-primary" to="/schedules/assignments" style={{ height: 30 }}>
                기사 배정
              </Link>
              <Link className="btn btn-ghost" to="/schedules/suspend" style={{ height: 30 }}>
                운행 중단 처리
              </Link>
            </div>
          </div>
          <table className="data-table">
            <thead>
              <tr>
                <th>순번</th>
                <th>노선</th>
                <th>시작</th>
                <th>종료</th>
                <th>배차 간격</th>
                <th>운행 횟수</th>
                <th>상태</th>
                <th>관리</th>
              </tr>
            </thead>
            <tbody>
              {pagedListRows.map((row) => (
                <tr key={row.route}>
                  <td>{row.no}</td>
                  <td>{row.route}</td>
                  <td>{row.start}</td>
                  <td>{row.end}</td>
                  <td>{row.interval}</td>
                  <td>{row.rounds}</td>
                  <td>
                    <StatusBadge tone={row.tone}>{row.status}</StatusBadge>
                  </td>
                  <td>
                    <Link
                      className="btn btn-outline"
                      to={`/schedules/detail?route=${encodeURIComponent(row.route)}&date=${selectedDateKey}&weekday=${weekday}`}
                      style={{ height: 28, fontSize: 12 }}
                    >
                      상세
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="pagination">
            <span>총 {listRows.length}건</span>
            {listRows.length > LIST_PAGE_SIZE ? (
              <div className="pagination-pages">
                {Array.from({ length: listPageCount }, (_, i) => i + 1).map((n) => (
                  <button
                    key={n}
                    type="button"
                    className={`page-chip${n === safeListPage ? ' active' : ''}`}
                    onClick={() => setListPage(n)}
                  >
                    {n}
                  </button>
                ))}
              </div>
            ) : null}
          </div>
        </section>

        <section className="card card-pad sched-card sched-timetable-card">
          <div className="card-head sched-timetable-head">
            <div className="sched-timetable-title-row">
              <h3>공식 시간표</h3>
              <select
                className="select sched-timetable-route-select"
                value={timetableRoute}
                onChange={(e) => {
                  const next = e.target.value
                  setTimetableRoute(next)
                  if (next === '기흥역 통학버스' && timetablePeriod === 'VACATION') {
                    setTimetablePeriod('SEMESTER')
                  }
                }}
                aria-label="상세 시간표 노선"
              >
                {SCHEDULE_ROUTE_OPTIONS.map((name) => (
                  <option key={name} value={name}>
                    {name}
                  </option>
                ))}
              </select>
            </div>
            <div className="toolbar" style={{ gap: 6 }}>
              <button
                type="button"
                className={`btn btn-xs ${timetablePeriod === 'SEMESTER' ? 'btn-primary' : 'btn-outline'}`}
                onClick={() => setTimetablePeriod('SEMESTER')}
              >
                학기 중
              </button>
              <button
                type="button"
                className={`btn btn-xs ${timetablePeriod === 'VACATION' ? 'btn-primary' : 'btn-outline'}`}
                onClick={() => {
                  if (timetableRoute === '기흥역 통학버스') {
                    setTimetableRoute('명지대역 셔틀')
                  }
                  setTimetablePeriod('VACATION')
                }}
              >
                방학·계절학기
              </button>
            </div>
          </div>
          <p className="muted" style={{ fontSize: 12, marginTop: 0 }}>
            {timetablePeriod === 'SEMESTER' ? '학기 중' : '방학·계절학기'} · {WEEKDAY_LABELS[weekday]}요일 ·{' '}
            {timetableRoute}
            {dbRouteSummary
              ? ` · ${dbRouteSummary.rounds}회 · ${dbRouteSummary.start}~${dbRouteSummary.end}`
              : timetableRoute === '기흥역 통학버스' && timetablePeriod === 'VACATION'
                ? ' · 기흥역은 방학·계절학기 미운행'
                : ' · 해당 요일 일정 없음'}
          </p>
          <div className="sched-timetable-scroll">
            <table className="data-table dense">
              <thead>
                <tr>
                  <th>#</th>
                  <th>노선</th>
                  <th>출발</th>
                  <th>요일</th>
                  <th>구분</th>
                </tr>
              </thead>
              <tbody>
                {(dbTrips.length
                  ? dbTrips
                  : mjuTrips.map((t, i) => ({
                      id: `local-${i}`,
                      departure_time: `${t.departure}:00`,
                      weekday: selectedWeekday,
                      semester: timetablePeriod,
                      routes: { route_name: t.route },
                    }))
                ).map((row, idx) => (
                  <tr key={'id' in row && row.id ? String(row.id) : `row-${idx}`}>
                    <td>{idx + 1}</td>
                    <td>{('routes' in row && row.routes?.route_name) || timetableRoute}</td>
                    <td style={{ fontWeight: 700 }}>
                      {'departure_time' in row ? formatTime(String(row.departure_time)) : '-'}
                    </td>
                    <td>{'weekday' in row ? String(row.weekday) : selectedWeekday}</td>
                    <td>
                      {('semester' in row ? String(row.semester) : timetablePeriod) === 'VACATION'
                        ? '방학·계절학기'
                        : '학기 중'}
                    </td>
                  </tr>
                ))}
                {!dbTrips.length && !mjuTrips.length ? (
                  <tr>
                    <td colSpan={5} className="muted">
                      {timetableRoute === '기흥역 통학버스' && timetablePeriod === 'VACATION'
                        ? '기흥역 통학버스는 방학·계절학기·주말에 운행하지 않습니다.'
                        : '해당 조건 시간표가 없습니다. 노선·학기중/방학·요일을 바꿔 보세요.'}
                    </td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </section>

        {/* 3행: 예외 ↔ 패턴 미리보기 (아래변 일치) */}
        <section className="card card-pad sched-card">
          <div className="card-head">
            <h3>예외 일정 관리</h3>
            <button className="btn btn-primary" type="button" style={{ height: 30, fontSize: 12 }}>
              + 예외 일정 등록
            </button>
          </div>
          <div className="toolbar" style={{ marginBottom: 10 }}>
            <button className="btn btn-ghost" type="button" style={{ height: 30 }}>
              예정된 예외
            </button>
            <button className="btn btn-outline" type="button" style={{ height: 30 }}>
              지난 예외
            </button>
          </div>
          <table className="data-table">
            <thead>
              <tr>
                <th>일자</th>
                <th>사유</th>
                <th>상태</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>{formatDotDate(addDays(weekStart, 3))}</td>
                <td>정기 점검</td>
                <td>
                  <StatusBadge tone="orange">확정</StatusBadge>
                </td>
              </tr>
              <tr>
                <td>{formatDotDate(addDays(weekStart, 5))}</td>
                <td>축제 일정으로 배차 증편</td>
                <td>
                  <StatusBadge tone="orange">확정</StatusBadge>
                </td>
              </tr>
            </tbody>
          </table>
        </section>

        <section className="card card-pad sched-card sched-pattern-card">
          <div className="card-head">
            <h3>운행 패턴 미리보기</h3>
            <span className="muted" style={{ fontSize: 12 }}>
              전체 보기
            </span>
          </div>
          <div className="pattern-bars" aria-hidden>
            {[40, 70, 95, 80, 55, 45, 60, 75, 50, 30].map((h, i) => (
              <div key={i} className="pattern-col">
                <span style={{ height: `${h}%` }} className="bar bar-a" />
                <span style={{ height: `${Math.max(18, h - 25)}%` }} className="bar bar-b" />
              </div>
            ))}
          </div>
          <div className="legend-row">
            <span>
              <i style={{ background: '#266ef4' }} /> 평일
            </span>
            <span>
              <i style={{ background: '#3fb46a' }} /> 주말
            </span>
          </div>
        </section>
      </div>
    </div>
  )
}

'''

path.write_text(text[:start] + new_return + text[end:], encoding="utf-8")
print("OK replaced return block")
