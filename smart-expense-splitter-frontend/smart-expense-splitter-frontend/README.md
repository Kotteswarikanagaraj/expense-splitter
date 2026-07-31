# Smart Expense Splitter — Frontend (Phase 3)

React (Vite) frontend for the Smart Expense Splitter backend.

## Setup
```bash
npm install
npm run dev
```
Runs at `http://localhost:5173`. Make sure the backend is running on `http://localhost:8080`
(CORS is already configured there for this exact origin).

## Folder structure
```
src/
├── api/               Thin wrappers over axios — one file per backend resource
│   ├── axiosClient.js   shared instance + JWT interceptor
│   ├── authApi.js
│   ├── groupApi.js
│   ├── expenseApi.js
│   └── settlementApi.js
├── context/
│   └── AuthContext.jsx  global auth state (user, login, register, logout)
├── hooks/
│   └── useGroupSocket.js  STOMP/SockJS subscription for one group's live updates
├── components/
│   ├── Navbar.jsx
│   ├── ProtectedRoute.jsx
│   ├── AddExpenseForm.jsx
│   ├── ExpenseList.jsx
│   ├── BalancesView.jsx
│   └── SettlementsView.jsx
├── pages/
│   ├── LoginPage.jsx
│   ├── RegisterPage.jsx
│   ├── GroupListPage.jsx
│   └── GroupDetailPage.jsx
├── App.jsx             route definitions
└── main.jsx             entry point (Router + AuthProvider wrap App)
```

## How each piece fits together (for interview defense)

- **`axiosClient.js`** is the ONLY place that knows about the JWT. Its request
  interceptor attaches `Authorization: Bearer <token>` to every outgoing
  request automatically, reading the token from `localStorage`. No component
  ever manually sets an auth header.

- **`AuthContext.jsx`** holds the logged-in user in React state, initialized
  from `localStorage` on load (so refreshing the page doesn't log you out —
  this is the entire "session persistence" story, matching the backend being
  fully stateless/JWT-based).

- **`ProtectedRoute.jsx`** wraps any route that needs login; if there's no
  user in context, it redirects to `/login` via React Router's `<Navigate>`.

- **`useGroupSocket.js`** is a custom hook that opens one STOMP-over-SockJS
  connection per mounted `GroupDetailPage` and subscribes to
  `/topic/group/{groupId}`. Cleanup on unmount (`client.deactivate()`)
  prevents leaking sockets when navigating between groups.

- **`GroupDetailPage.jsx`** is the orchestrator: it owns a `refreshSignal`
  counter. Three different triggers bump it — adding an expense, settling a
  debt, or a WebSocket message arriving — and `BalancesView`,
  `SettlementsView`, and `ExpenseList` all refetch themselves whenever it
  changes. This "bump a counter, let children refetch themselves" pattern
  keeps each child self-contained (owns its own loading/error state) instead
  of the parent having to fetch everything and pass data down.

- **`AddExpenseForm.jsx`** adapts its own fields based on the selected split
  type: EQUAL sends no per-member data (server computes it), EXACT/PERCENTAGE
  render one input per member and send `shares: [{ userId, value }]`.

## What's simplified/deferred in this scaffold
- "Add member" takes a raw user ID, not an email lookup — a real app would
  add a `GET /api/users/search?email=` endpoint and autocomplete against it.
- No toast/notification library — errors render inline as plain text.
- No global loading spinner component; each view manages its own `loading` state.
- No token refresh — when the JWT expires, the axios response interceptor
  clears storage and bounces the user to `/login`.
