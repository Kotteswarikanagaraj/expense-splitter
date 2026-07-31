import { useState } from 'react';
import * as expenseApi from '../api/expenseApi';
import { useAuth } from '../context/AuthContext';

/**
 * `members` = the group's member list, used only for the per-member share
 * inputs on EXACT/PERCENTAGE splits. The payer is no longer chosen from a
 * dropdown — it's always the logged-in user, decided server-side too (never
 * trust the client to say who paid).
 */
export default function AddExpenseForm({ groupId, members, onExpenseAdded }) {
  const { user } = useAuth();
  const [description, setDescription] = useState('');
  const [amount, setAmount] = useState('');
  const [splitType, setSplitType] = useState('EQUAL');
  const [sharesByUser, setSharesByUser] = useState({});
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleShareChange = (userId, value) => {
    setSharesByUser((prev) => ({ ...prev, [userId]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);

    const payload = {
      groupId,
      description,
      amount: Number(amount),
      splitType,
    };

    if (splitType !== 'EQUAL') {
      payload.shares = members.map((m) => ({
        userId: m.userId,
        value: Number(sharesByUser[m.userId] || 0),
      }));
    }

    try {
      await expenseApi.addExpense(payload);
      setDescription('');
      setAmount('');
      setSharesByUser({});
      onExpenseAdded();
    } catch (err) {
      setError(err.response?.data?.message || 'Could not add expense.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="card add-expense-form">
      <h3>Add Expense</h3>
      {error && <p className="error-text">{error}</p>}

      <p className="muted">You're adding this as: <strong>{user?.name}</strong></p>

      <label>
        Description
        <input value={description} onChange={(e) => setDescription(e.target.value)} required />
      </label>

      <label>
        Amount
        <input
          type="number"
          step="0.01"
          min="0.01"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          required
        />
      </label>

      <label>
        Split type
        <select value={splitType} onChange={(e) => setSplitType(e.target.value)}>
          <option value="EQUAL">Equal</option>
          <option value="EXACT">Exact amounts</option>
          <option value="PERCENTAGE">Percentage</option>
        </select>
      </label>

      {splitType !== 'EQUAL' && (
        <div className="shares-input">
          <p>{splitType === 'EXACT' ? 'Exact amount per member:' : 'Percentage per member (must total 100):'}</p>
          {members.map((m) => (
            <label key={m.userId} className="share-input-row">
              {m.name}
              <input
                type="number"
                step="0.01"
                min="0"
                value={sharesByUser[m.userId] || ''}
                onChange={(e) => handleShareChange(m.userId, e.target.value)}
                required
              />
            </label>
          ))}
        </div>
      )}

      <button type="submit" disabled={submitting}>
        {submitting ? 'Adding...' : 'Add Expense'}
      </button>
    </form>
  );
}