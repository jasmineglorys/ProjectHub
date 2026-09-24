import { createContext, useContext, useEffect, useMemo, useState, useCallback } from 'react';
import { loginUser, registerUser, fetchCurrentUser } from '../api/authApi';
import { extractError } from '../api/axiosConfig';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [initialising, setInitialising] = useState(true);

  // Restore the session from localStorage, then re-validate against the backend.
  useEffect(() => {
    const stored = localStorage.getItem('ph_user');
    const token = localStorage.getItem('ph_token');

    if (!stored || !token) {
      setInitialising(false);
      return;
    }

    setUser(JSON.parse(stored));

    fetchCurrentUser()
      .then((res) => {
        setUser(res.data);
        localStorage.setItem('ph_user', JSON.stringify(res.data));
      })
      .catch(() => {
        localStorage.removeItem('ph_user');
        localStorage.removeItem('ph_token');
        setUser(null);
      })
      .finally(() => setInitialising(false));
  }, []);

  const persist = useCallback((data) => {
    localStorage.setItem('ph_token', data.token);
    localStorage.setItem('ph_user', JSON.stringify(data.user));
    setUser(data.user);
  }, []);

  const login = useCallback(
    async (credentials) => {
      try {
        const res = await loginUser(credentials);
        persist(res.data);
        return { ok: true, user: res.data.user };
      } catch (error) {
        return { ok: false, message: extractError(error, 'Invalid email or password.') };
      }
    },
    [persist],
  );

  const register = useCallback(
    async (payload) => {
      try {
        const res = await registerUser(payload);
        persist(res.data);
        return { ok: true, user: res.data.user };
      } catch (error) {
        return { ok: false, message: extractError(error, 'Registration failed.') };
      }
    },
    [persist],
  );

  const logout = useCallback(() => {
    localStorage.removeItem('ph_token');
    localStorage.removeItem('ph_user');
    setUser(null);
  }, []);

  /** Keeps liked/bookmarked id lists in sync after a toggle. */
  const updateUserLists = useCallback((field, projectId, active) => {
    setUser((prev) => {
      if (!prev) return prev;
      const current = prev[field] || [];
      const next = active
        ? [...new Set([...current, projectId])]
        : current.filter((id) => id !== projectId);
      const updated = { ...prev, [field]: next };
      localStorage.setItem('ph_user', JSON.stringify(updated));
      return updated;
    });
  }, []);

  const value = useMemo(
    () => ({
      user,
      isAuthenticated: Boolean(user),
      isAdmin: user?.role === 'ADMIN',
      initialising,
      login,
      register,
      logout,
      updateUserLists,
    }),
    [user, initialising, login, register, logout, updateUserLists],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used inside an AuthProvider');
  }
  return ctx;
}
