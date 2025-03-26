import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { HomeComponent } from './components/navbar/home.component';
import { AboutComponent } from './components/navbar/about.component';
import { RegisterComponent } from './components/user/register.component';
import { LoginComponent } from './components/user/login.component';
import { TrendingComponent } from './components/navbar/trending.component';
import { WatchlistComponent } from './components/stocks/watchlist.component';
import { StockComponent } from './components/stocks/stock.component';
import { AuthGuard } from './services/auth.guard';
import { NotificationSettingsComponent } from './components/user/notification-settings.component';

const routes: Routes = [
  { path: 'home', component: HomeComponent },
  { path: 'about', component: AboutComponent },
  { path: 'login', component: LoginComponent },
  { path: 'signup', component: RegisterComponent },
  { path: 'trending', component: TrendingComponent },
  { path: 'stock/:ticker', component: StockComponent },
  { path: 'watchlist', component: WatchlistComponent, canActivate: [AuthGuard] },
  { path: 'notifications', component: NotificationSettingsComponent},
  { path: '', redirectTo: '/home', pathMatch: 'full' }
];


@NgModule({
  declarations: [],
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }