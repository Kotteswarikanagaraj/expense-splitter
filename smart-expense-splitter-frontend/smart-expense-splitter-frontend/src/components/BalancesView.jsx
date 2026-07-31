import { useEffect, useState } from 'react';
import * as settlementApi from '../api/settlementApi';

/**
 * Shows each member's raw net balance (positive = owed money, negative = owes
 * money) — this is the "before simplification" view, distinct from
 * SettlementsView which shows the minimal payment plan.
 */
export default function BalancesView({ groupId, refreshSignal }) {
  const [balances, setBalances] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    settlementApi
      .getBalances(groupId)
      .then((res) => {
        if (!cancelled) setBalances(res.data);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [groupId, refreshSignal]);

  if (loading) return <div className="card"><h3>Balances</h3><p>Loading...</p></div>;

  return (
    <div className="card">
      <h3>Balances</h3>
      <ul className="balance-list">
        {balances.map((b) => (
          <li key={b.userId} className={b.balance >= 0 ? 'positive' : 'negative'}>
            {b.userName}:{' '}
            {b.balance > 0 && `is owed ₹${b.balance}`}
            {b.balance < 0 && `owes ₹${Math.abs(b.balance)}`}
            {b.balance === 0 && 'settled up'}
          </li>
        ))}
      </ul>
    </div>
  );
}
