import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import * as groupApi from '../api/groupApi';
import Navbar from '../components/Navbar';
import ExpenseList from '../components/ExpenseList';
import AddExpenseForm from '../components/AddExpenseForm';
import BalancesView from '../components/BalancesView';
import SettlementsView from '../components/SettlementsView';
import { useGroupSocket } from '../hooks/useGroupSocket';
import { useAuth } from '../context/AuthContext';

export default function GroupDetailPage() {
  const { groupId } = useParams();
  const { user } = useAuth();
  const [group, setGroup] = useState(null);
  const [newMemberEmail, setNewMemberEmail] = useState('');
  const [memberError, setMemberError] = useState('');
  const [refreshSignal, setRefreshSignal] = useState(0);

  // Each entry: { id, text }. A "payment request" that showed up live over
  // the WebSocket — dismissible, not persisted anywhere (purely a UI nudge).
  const [paymentRequests, setPaymentRequests] = useState([]);

  const loadGroup = useCallback(async () => {
    const res = await groupApi.getGroup(groupId);
    setGroup(res.data);
  }, [groupId]);

  useEffect(() => {
    loadGroup();
  }, [loadGroup]);

  const triggerRefresh = () => setRefreshSignal((n) => n + 1);

  const handleSocketMessage = useCallback((message) => {
    triggerRefresh();

    // Payment-request notice: only relevant for EXPENSE_ADDED events where
    // the current user is one of the people who owes a share, and isn't the
    // one who paid (no point notifying yourself that you owe yourself money).
    if (message.type === 'EXPENSE_ADDED' && message.data) {
      const expense = message.data;
      const isPayer = expense.paidBy === user?.userId;
      const myShare = expense.shares.find((s) => s.userId === user?.userId);

      if (!isPayer && myShare && Number(myShare.shareAmount) > 0) {
        const notice = {
          id: `${expense.id}-${Date.now()}`,
          text: `${expense.paidByName} added "${expense.description}" — you owe ₹${myShare.shareAmount}`,
        };
        setPaymentRequests((prev) => [...prev, notice]);
      }
    }
  }, [user]);

  useGroupSocket(groupId, handleSocketMessage);

  const dismissRequest = (id) => {
    setPaymentRequests((prev) => prev.filter((r) => r.id !== id));
  };

  const handleAddMember = async (e) => {
    e.preventDefault();
    setMemberError('');
    try {
      await groupApi.addMember(groupId, Number(newMemberEmail));
      setNewMemberEmail('');
      await loadGroup();
    } catch (err) {
      setMemberError(err.response?.data?.message || 'Could not add member.');
    }
  };

  if (!group) return <><Navbar /><p className="page">Loading group...</p></>;

  return (
    <>
      <Navbar />
      <div className="page">
        <h1>{group.name}</h1>
        <p className="muted">Created by {group.createdByName}</p>

        {/* Payment-request notices — live, dismissible, one per new expense
            that affects the current user */}
        {paymentRequests.map((req) => (
          <div key={req.id} className="card payment-request-banner">
            <span>{req.text}</span>
            <button onClick={() => dismissRequest(req.id)}>Dismiss</button>
          </div>
        ))}

        <div className="card">
          <h3>Members</h3>
          <ul>
            {group.members.map((m) => (
              <li key={m.userId}>{m.name} ({m.email})</li>
            ))}
          </ul>
          <form onSubmit={handleAddMember} className="inline-form">
            <input
              placeholder="User ID to add"
              value={newMemberEmail}
              onChange={(e) => setNewMemberEmail(e.target.value)}
            />
            <button type="submit">Add Member</button>
          </form>
          {memberError && <p className="error-text">{memberError}</p>}
        </div>

        <AddExpenseForm groupId={Number(groupId)} members={group.members} onExpenseAdded={triggerRefresh} />

        <div className="grid-two">
          <BalancesView groupId={groupId} refreshSignal={refreshSignal} />
          <SettlementsView groupId={groupId} refreshSignal={refreshSignal} onSettled={triggerRefresh} />
        </div>

        <ExpenseList groupId={groupId} refreshSignal={refreshSignal} />
      </div>
    </>
  );
}