import { Injectable } from "@angular/core";
import { ComponentStore } from "@ngrx/component-store";
import { User, UserState } from "./model/user";
import { UserService } from "./services/user.service";

const INIT_STATE: UserState = {
    currentUser: null,
    isLoggedIn: false,
    isLoading: false,
    error: null
}
@Injectable()
export class UserStore extends ComponentStore<UserState> {

    constructor() {
        super(INIT_STATE);}
        
        // Selectors
    readonly currentUser$ = this.select(state => state.currentUser);
    readonly isLoggedIn$ = this.select(state => state.isLoggedIn);
    readonly isLoading$ = this.select(state => state.isLoading);
    readonly error$ = this.select(state => state.error);
    readonly watchlist$ = this.select(state => state.currentUser?.watchlist || new Set<string>());

    // Updaters
    readonly setCurrentUser = this.updater((state, user: User | null) => ({
        ...state,
        currentUser: user
    }));

    readonly setLoggedIn = this.updater((state, isLoggedIn: boolean) => ({
        ...state,
        isLoggedIn
    }));

    readonly setLoading = this.updater((state, isLoading: boolean) => ({
        ...state,
        isLoading
    }));

    readonly setError = this.updater((state, error: string | null) => ({
        ...state,
        error
    }));
}







