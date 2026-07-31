import { useEffect, useState } from 'react';
import * as settlementApi from '../api/settlementApi';

/**
 * Shows the minimal set of suggested payments (from the greedy debt-
 * simplification algorithm) and lets a member mark one as paid.
 */
export default function SettlementsView({ groupId, refreshSignal, onSettled }) {
  const [settlements, setSettlements] = useState([]);
  const [loading, setLoading] = useState(true);
  const [settlingId, setSettlingId] = useState(null); // tracks which button is mid-click

  const loadSettlements = async () => {
    setLoading(true);
    try {
      const res = await settlementApi.getSettlements(groupId);
      setSettlements(res.data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSettlements();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [groupId, refreshSignal]);

  const handleSettle = async (settlementId) => {
    setSettlingId(settlementId);
    try {
      await settlementApi.settleUp(groupId, settlementId);
      await loadSettlements(); // refetch so the settled one drops off / updates
      onSettled?.(); // let the parent know balances should refresh too
    } finally {
      setSettlingId(null);
    }
  };

  if (loading) return <div className="card"><h3>Suggested Settlements</h3><p>Loading...</p></div>;

  return (
    <div className="card">
      <h3>Suggested Settlements</h3>
      {settlements.length === 0 ? (
        <p>Everyone's settled up 🎉</p>
      ) : (
        <ul className="settlement-list">
          {settlements.map((s) => (
            <li key={s.id} className={s.settled ? 'settled' : ''}>
              <span>
                {s.fromUserName} → {s.toUserName}: ₹{s.amount}
              </span>
              {!s.settled && (
                <button onClick={() => handleSettle(s.id)} disabled={settlingId === s.id}>
                  {settlingId === s.id ? 'Marking...' : 'Mark as paid'}
                </button>
              )}
              {s.settled && <span className="badge">Paid</span>}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
