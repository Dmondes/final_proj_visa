export interface User {
    id: number;
    email: string;
    watchlist: Set<string>;
    priceAlerts?: { [ticker: string]: PriceAlert };
}

export interface UserState{
    currentUser: User | null;
    isLoggedIn: boolean;
    isLoading: boolean;
    error: string | null;
}

export interface PriceAlert {
    ticker: string;
    targetPrice: number;
    condition: 'above' | 'below';
    createdAt: number;
}