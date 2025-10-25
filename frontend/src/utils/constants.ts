import DirectionsCarIcon from '@mui/icons-material/DirectionsCar';
import VerifiedIcon from '@mui/icons-material/Verified';
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import SecurityIcon from '@mui/icons-material/Security';
import PublicIcon from '@mui/icons-material/Public';

export const features = [
  {
    title: 'Connect Your EV',
    description: 'Sync your electric vehicle data automatically through OEM APIs or OBD-II devices. Track every kilometer driven.',
    icon: DirectionsCarIcon,
    color: '#4caf50',
  },
  {
    title: 'Get Verified Credits',
    description: 'Your CO₂ savings are verified by certified auditors and converted into tradable carbon credits following international standards.',
    icon: VerifiedIcon,
    color: '#2196f3',
  },
  {
    title: 'Trade on Marketplace',
    description: 'List your carbon credits at fixed prices or through auctions. Connect with corporate buyers seeking verified offsets.',
    icon: AccountBalanceWalletIcon,
    color: '#ff9800',
  },
  {
    title: 'Earn Passive Income',
    description: 'Generate 5-7 million VND annually per vehicle. Get AI-powered pricing recommendations to maximize earnings.',
    icon: TrendingUpIcon,
    color: '#9c27b0',
  },
  {
    title: 'Secure Transactions',
    description: 'All transactions are protected by escrow. Payments are released only after successful credit transfer.',
    icon: SecurityIcon,
    color: '#f44336',
  },
  {
    title: 'Make an Impact',
    description: 'Contribute to Vietnam\'s Net Zero 2050 goal. Every credit traded represents real environmental impact.',
    icon: PublicIcon,
    color: '#00bcd4',
  },
];

export const APP_NAME = 'Carbon Credit Marketplace';
export const APP_VERSION = '1.0.0';

export const ROLES = {
  EV_OWNER: 'evowner',
  BUYER: 'buyer',
  VERIFIER: 'verifier',
  ADMIN: 'admin',
} as const;

export const KYC_LEVELS = {
  UNVERIFIED: 0,
  BASIC: 1,
  ADVANCED: 2,
} as const;

export const TRANSACTION_STATUS = {
  PENDING: 'pending',
  PROCESSING: 'processing',
  COMPLETED: 'completed',
  FAILED: 'failed',
  CANCELLED: 'cancelled',
  REFUNDED: 'refunded',
} as const;

export const LISTING_TYPES = {
  FIXED: 'fixed',
  AUCTION: 'auction',
} as const;

export const PAYMENT_METHODS = {
  MOMO: 'momo',
  VNPAY: 'vnpay',
  ZALOPAY: 'zalopay',
  BANK_TRANSFER: 'bank_transfer',
  STRIPE: 'stripe',
} as const;
