import { useEffect, useState } from 'react';
import * as expenseApi from '../api/expenseApi';

/**
 * Displays a paginated, filterable list of expenses for one group.
 * `refreshSignal` is a prop that changes whenever the parent wants this list
 * to refetch (e.g. after a new expense is added, or a WebSocket event comes
 * in) — see the effect below reacting to it.
 */
export default function ExpenseList({ groupId, refreshSignal }) {
  const [expenses, setExpenses] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [splitTypeFilter, setSplitTypeFilter] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false; // avoids setting state on an unmounted/stale request

    const load = async () => {
      setLoading(true);
      try {
        const response = await expenseApi.getExpenses(groupId, {
          page,
          size: 10,
          splitType: splitTypeFilter || undefined, // undefined keys are dropped by axios
        });
        if (!cancelled) {
          setExpenses(response.data.content);
          setTotalPages(response.data.totalPages);
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    };

    load();
    return () => {
      cancelled = true;
    };
  }, [groupId, page, splitTypeFilter, refreshSignal]);

  return (
    <div className="card">
      <div className="card-header">
        <h3>Expenses</h3>
        <select
          value={splitTypeFilter}
          onChange={(e) => {
            setPage(0); // reset to first page whenever the filter changes
            setSplitTypeFilter(e.target.value);
          }}
        >
          <option value="">All split types</option>
          <option value="EQUAL">Equal</option>
          <option value="EXACT">Exact</option>
          <option value="PERCENTAGE">Percentage</option>
        </select>
      </div>

      {loading ? (
        <p>Loading...</p>
      ) : expenses.length === 0 ? (
        <p>No expenses yet.</p>
      ) : (
        <ul className="expense-list">
          {expenses.map((exp) => (
            <li key={exp.id}>
              <div className="expense-row">
                <span>
                  <strong>{exp.description}</strong> — ₹{exp.amount} ({exp.splitType})
                </span>
                <span>paid by {exp.paidByName}</span>
              </div>
              <div className="expense-shares">
                {exp.shares.map((s) => (
                  <span key={s.userId} className="share-chip">
                    {s.userName}: ₹{s.shareAmount}
                  </span>
                ))}
              </div>
            </li>
          ))}
        </ul>
      )}

      {totalPages > 1 && (
        <div className="pagination">
          <button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            Prev
          </button>
          <span>Page {page + 1} of {totalPages}</span>
          <button disabled={page + 1 >= totalPages} onClick={() => setPage((p) => p + 1)}>
            Next
          </button>
        </div>
      )}
    </div>
  );
}
