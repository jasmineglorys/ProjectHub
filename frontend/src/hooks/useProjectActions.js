import { useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { toggleLike, toggleBookmark } from '../api/projectApi';
import { extractError } from '../api/axiosConfig';
import { useAuth } from '../context/AuthContext';

/**
 * Shared like/bookmark handlers. `applyChange(projectId, patch)` lets each page
 * update whatever local state it holds (a list, a single project, etc).
 */
export default function useProjectActions(applyChange, onError) {
  const { isAuthenticated, updateUserLists } = useAuth();
  const navigate = useNavigate();

  const like = useCallback(
    async (projectId) => {
      if (!isAuthenticated) {
        navigate('/login');
        return;
      }
      try {
        const res = await toggleLike(projectId);
        applyChange(projectId, { likes: res.data.likes, likedByMe: res.data.active });
        updateUserLists('likedProjects', projectId, res.data.active);
      } catch (error) {
        onError?.(extractError(error, 'Could not update the like.'));
      }
    },
    [isAuthenticated, navigate, applyChange, updateUserLists, onError],
  );

  const bookmark = useCallback(
    async (projectId) => {
      if (!isAuthenticated) {
        navigate('/login');
        return;
      }
      try {
        const res = await toggleBookmark(projectId);
        applyChange(projectId, { bookmarkedByMe: res.data.active });
        updateUserLists('bookmarks', projectId, res.data.active);
      } catch (error) {
        onError?.(extractError(error, 'Could not update the bookmark.'));
      }
    },
    [isAuthenticated, navigate, applyChange, updateUserLists, onError],
  );

  return { like, bookmark };
}
