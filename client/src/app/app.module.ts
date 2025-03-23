import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { ReactiveFormsModule } from '@angular/forms';
import { AppComponent } from './app.component';
import { AppRoutingModule } from './app-routing.module';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HomeComponent } from './components/navbar/home.component';
import { NavbarComponent } from './components/navbar/navbar.component';
import { AboutComponent } from './components/navbar/about.component';
import { LoginComponent } from './components/user/login.component';
import { RegisterComponent } from './components/user/register.component';
import { TrendingComponent } from './components/navbar/trending.component';
import { WatchlistComponent } from './components/stocks/watchlist.component';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { StockComponent } from './components/stocks/stock.component';
import { LocationStrategy, HashLocationStrategy } from '@angular/common'; //hash routing for railway
import { UserStore } from './user.store';

@NgModule({
  declarations: [
    AppComponent,
    HomeComponent,
    NavbarComponent,
    AboutComponent,
    LoginComponent,
    RegisterComponent,
    TrendingComponent,
    WatchlistComponent,
    StockComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    ReactiveFormsModule,
    NgbModule
    
  ],
  providers: [
    provideHttpClient(withInterceptorsFromDi()),
    { provide: LocationStrategy, useClass: HashLocationStrategy },
    UserStore
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
