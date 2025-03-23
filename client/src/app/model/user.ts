export interface User {
    id: number;
    email: string;
    watchlist: Set<string>;
}

export interface UserState{
    currentUser: User | null;
    isLoggedIn: boolean;
    isLoading: boolean;
    error: string | null;
}