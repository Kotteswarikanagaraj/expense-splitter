import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import * as groupApi from '../api/groupApi';
import Navbar from '../components/Navbar';

export default function GroupListPage() {
  const [groups, setGroups] = useState([]);
  const [newGroupName, setNewGroupName] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Extracted so both the initial load AND "after creating a group" can
  // reuse the exact same fetch-and-set logic — avoids duplicating it.
  const loadGroups = async () => {
    try {
      const response = await groupApi.getMyGroups();
      setGroups(response.data);
    } catch (err) {
      setError('Could not load your groups.');
    } finally {
      setLoading(false);
    }
  };

  // Empty dependency array = run once, right after the component first mounts.
  // This is the standard "fetch data on page load" pattern.
  useEffect(() => {
    loadGroups();
  }, []);

  const handleCreateGroup = async (e) => {
    e.preventDefault();
    if (!newGroupName.trim()) return;
    try {
      await groupApi.createGroup({ name: newGroupName });
      setNewGroupName('');
      await loadGroups(); // refetch so the new group shows up in the list
    } catch (err) {
      setError('Could not create group.');
    }
  };

  return (
    <>
      <Navbar />
      <div className="page">
        <h1>Your Groups</h1>
        {error && <p className="error-text">{error}</p>}

        <form onSubmit={handleCreateGroup} className="inline-form">
          <input
            placeholder="New group name (e.g. Goa Trip)"
            value={newGroupName}
            onChange={(e) => setNewGroupName(e.target.value)}
          />
          <button type="submit">Create Group</button>
        </form>

        {loading ? (
          <p>Loading...</p>
        ) : groups.length === 0 ? (
          <p>You're not in any groups yet. Create one above to get started.</p>
        ) : (
          <ul className="group-list">
            {groups.map((group) => (
              <li key={group.id}>
                <Link to={`/groups/${group.id}`}>
                  <strong>{group.name}</strong>
                  <span> — {group.members.length} member{group.members.length !== 1 ? 's' : ''}</span>
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>
    </>
  );
}
