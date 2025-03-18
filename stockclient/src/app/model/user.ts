export interface User {
    id: number;
    email: string;
    watchlist: Set<string>;
}