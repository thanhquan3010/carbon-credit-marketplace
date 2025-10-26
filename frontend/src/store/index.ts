import { configureStore } from '@reduxjs/toolkit';
import authReducer from './slices/authSlice';
import marketplaceReducer from './slices/marketplaceSlice';

export const store = configureStore({
  reducer: {
    auth: authReducer,
    marketplace: marketplaceReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
