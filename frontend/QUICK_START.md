# Frontend Quick Start Guide

## Prerequisites

- Node.js 18.x or higher
- npm or yarn package manager
- Backend API running (see backend documentation)

## Installation

1. **Navigate to frontend directory**
   ```bash
   cd frontend
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Set up environment variables**
   
   Create `.env.local` file in the frontend directory:
   ```env
   NEXT_PUBLIC_API_URL=http://localhost:8080/api
   NEXT_PUBLIC_WS_URL=http://localhost:8080/ws
   ```

4. **Update _app.tsx**
   
   Rename `src/pages/_app.example.tsx` to `src/pages/_app.tsx` or merge the content:
   ```bash
   # On Windows PowerShell
   Copy-Item src/pages/_app.example.tsx src/pages/_app.tsx
   
   # On Linux/Mac
   cp src/pages/_app.example.tsx src/pages/_app.tsx
   ```

## Running the Development Server

```bash
npm run dev
```

The application will be available at `http://localhost:3000`

## Project Structure

```
frontend/
├── src/
│   ├── components/          # Reusable UI components
│   │   ├── auth/           # Authentication components
│   │   ├── marketplace/    # Marketplace components
│   │   ├── dashboard/      # Dashboard components
│   │   └── common/         # Shared components
│   ├── hooks/              # Custom React hooks
│   ├── services/           # API service layer
│   ├── store/              # Redux state management
│   ├── pages/              # Next.js pages
│   ├── styles/             # Global styles and theme
│   └── utils/              # Utility functions
├── public/                 # Static assets
├── .env.local             # Environment variables (create this)
└── package.json           # Dependencies
```

## Available Scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run start` - Start production server
- `npm run lint` - Run ESLint
- `npm run type-check` - Run TypeScript compiler check
- `npm run format` - Format code with Prettier
- `npm test` - Run tests

## Creating Pages

### Example: Marketplace Page

1. Create page file: `src/pages/marketplace/index.tsx`
2. Use the example as reference: `src/pages/marketplace/index.example.tsx`
3. Import and use the marketplace components

```tsx
import { ListingCard } from '../../components/marketplace/ListingCard';
import { SearchFilters } from '../../components/marketplace/SearchFilters';
import { PriceChart } from '../../components/marketplace/PriceChart';
```

### Example: Dashboard Page

1. Create page file: `src/pages/dashboard/index.tsx`
2. Use the example as reference: `src/pages/dashboard/index.example.tsx`
3. Import and use the dashboard components

```tsx
import { CO2Widget } from '../../components/dashboard/CO2Widget';
import { EarningsChart } from '../../components/dashboard/EarningsChart';
import { TripHistory } from '../../components/dashboard/TripHistory';
```

## Using Custom Hooks

### Authentication

```tsx
import { useAuth } from '../hooks/useAuth';

function MyComponent() {
  const { user, login, logout, isAuthenticated } = useAuth();
  
  const handleLogin = async () => {
    const result = await login('user@example.com', 'password');
    if (result.success) {
      console.log('Logged in!');
    }
  };
  
  return (
    <div>
      {isAuthenticated ? (
        <p>Welcome, {user?.email}</p>
      ) : (
        <button onClick={handleLogin}>Login</button>
      )}
    </div>
  );
}
```

### WebSocket for Real-time Auctions

```tsx
import { useAuctionWebSocket } from '../hooks/useWebSocket';

function AuctionComponent({ listingId }) {
  const { currentBid, connected, placeBid } = useAuctionWebSocket(listingId);
  
  const handleBid = () => {
    placeBid(100.00);
  };
  
  return (
    <div>
      <p>Status: {connected ? 'Connected' : 'Disconnected'}</p>
      <p>Current Bid: ${currentBid?.amount}</p>
      <button onClick={handleBid}>Place Bid</button>
    </div>
  );
}
```

### Notifications

```tsx
import { useNotification } from '../hooks/useNotification';

function MyComponent() {
  const { showSuccess, showError, showWarning, showInfo } = useNotification();
  
  const handleAction = async () => {
    try {
      // Perform action
      showSuccess('Action completed successfully!');
    } catch (error) {
      showError('Failed to complete action');
    }
  };
  
  return <button onClick={handleAction}>Do Something</button>;
}
```

## Accessing Redux State

```tsx
import { useSelector, useDispatch } from 'react-redux';
import { RootState } from '../store';
import { addToWatchlist } from '../store/slices/marketplaceSlice';

function MyComponent() {
  const dispatch = useDispatch();
  const { listings, filters } = useSelector((state: RootState) => state.marketplace);
  const { user } = useSelector((state: RootState) => state.auth);
  
  const handleAddToWatchlist = (listingId: string) => {
    dispatch(addToWatchlist(listingId));
  };
  
  return <div>{/* Your JSX */}</div>;
}
```

## API Services

### Making API Calls

```tsx
import { marketplaceService } from '../services/marketplaceService';

async function fetchListings() {
  try {
    const response = await marketplaceService.getListings({
      projectType: ['Renewable Energy'],
      priceRange: { min: 0, max: 100 },
      page: 1,
      limit: 20,
    });
    
    console.log('Listings:', response.listings);
  } catch (error) {
    console.error('Error:', error);
  }
}
```

### Authentication API

```tsx
import { authService } from '../services/authService';

async function registerUser() {
  try {
    const response = await authService.register({
      email: 'user@example.com',
      username: 'username',
      password: 'SecureP@ss123',
      firstName: 'John',
      lastName: 'Doe',
      accountType: 'BUYER',
    });
    
    console.log('Registered:', response);
  } catch (error) {
    console.error('Registration failed:', error);
  }
}
```

## Common Issues & Solutions

### Issue: WebSocket Connection Failed

**Solution**: Ensure the backend WebSocket server is running and the `NEXT_PUBLIC_WS_URL` is correct in `.env.local`

### Issue: API Requests Failing with 401

**Solution**: Check if the JWT token is valid. Clear localStorage and log in again:
```javascript
localStorage.removeItem('token');
localStorage.removeItem('refreshToken');
```

### Issue: CORS Errors

**Solution**: Configure CORS in the backend to allow requests from `http://localhost:3000`

### Issue: Components Not Rendering

**Solution**: 
1. Check browser console for errors
2. Verify all dependencies are installed: `npm install`
3. Restart development server: `npm run dev`

### Issue: TypeScript Errors

**Solution**: Run type checking to see all errors:
```bash
npm run type-check
```

## Testing

### Running Tests

```bash
npm test
```

### Writing Tests

Example test for a component:

```tsx
import { render, screen } from '@testing-library/react';
import { ListingCard } from '../components/marketplace/ListingCard';

test('renders listing card', () => {
  const mockListing = {
    id: '1',
    // ... other properties
  };
  
  render(<ListingCard listing={mockListing} />);
  expect(screen.getByText(mockListing.credit.projectName)).toBeInTheDocument();
});
```

## Building for Production

1. **Create production build**
   ```bash
   npm run build
   ```

2. **Start production server**
   ```bash
   npm start
   ```

3. **Production environment variables**
   
   Create `.env.production` file:
   ```env
   NEXT_PUBLIC_API_URL=https://api.yourdomain.com/api
   NEXT_PUBLIC_WS_URL=wss://api.yourdomain.com/ws
   ```

## Deployment

### Vercel (Recommended for Next.js)

1. Install Vercel CLI:
   ```bash
   npm i -g vercel
   ```

2. Deploy:
   ```bash
   vercel
   ```

3. Set environment variables in Vercel dashboard

### Docker

1. Create `Dockerfile`:
   ```dockerfile
   FROM node:18-alpine
   WORKDIR /app
   COPY package*.json ./
   RUN npm install
   COPY . .
   RUN npm run build
   EXPOSE 3000
   CMD ["npm", "start"]
   ```

2. Build and run:
   ```bash
   docker build -t carbon-marketplace-frontend .
   docker run -p 3000:3000 carbon-marketplace-frontend
   ```

## Additional Resources

- [Next.js Documentation](https://nextjs.org/docs)
- [Material-UI Documentation](https://mui.com/material-ui/getting-started/)
- [Redux Toolkit Documentation](https://redux-toolkit.js.org/)
- [React Hook Form Documentation](https://react-hook-form.com/)
- [Recharts Documentation](https://recharts.org/)

## Support

For issues or questions:
1. Check the [IMPLEMENTATION_SUMMARY.md](./IMPLEMENTATION_SUMMARY.md) for component documentation
2. Review example pages in `src/pages/*.example.tsx`
3. Check component prop types in the source files
4. Review the backend API documentation

## Next Steps

1. ✅ Set up environment variables
2. ✅ Update `_app.tsx` with providers
3. ✅ Create pages using the example files
4. ✅ Connect to backend API
5. ✅ Test authentication flow
6. ✅ Test WebSocket connections
7. ✅ Customize theme and styling
8. ✅ Add additional features as needed
9. ✅ Write tests
10. ✅ Deploy to production

---

**Happy Coding! 🚀**
