import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { UserManager, WebStorageStateStore, type User } from 'oidc-client-ts';

interface AuthConfig { enabled:boolean; authority:string; clientId:string; }
@Injectable({providedIn:'root'})
export class AdminAuth {
 private readonly http=inject(HttpClient);
 private manager?:UserManager;
 readonly user=signal<User|null>(null);
 readonly ready=signal(false);
 readonly error=signal('');
 async initialize(){
  try{
   const config=await firstValueFrom(this.http.get<AuthConfig>('/api/v1/auth/config'));
   if(!config.enabled || !config.authority || !config.clientId){
    this.error.set('Administrator sign-in is not configured.');return;
   }
   this.manager=new UserManager({
    authority:config.authority,client_id:config.clientId,
    redirect_uri:window.location.origin+'/auth/callback',
    post_logout_redirect_uri:window.location.origin+'/',
    response_type:'code',scope:'openid profile email',
    userStore:new WebStorageStateStore({store:window.sessionStorage}),
    automaticSilentRenew:false
   });
   if(window.location.pathname==='/auth/callback'){
    await this.manager.signinRedirectCallback();
    window.history.replaceState({},'', '/');
   }
   this.user.set(await this.manager.getUser());
  }catch{
   this.error.set('Sign-in is not available. Check the identity provider settings.');
  }finally{this.ready.set(true);}
 }
 async login(){if(this.manager) await this.manager.signinRedirect();}
 async logout(){if(this.manager) await this.manager.signoutRedirect();}
 async accessToken():Promise<string|null>{
  const user=await this.manager?.getUser();
  return user && !user.expired ? user.access_token : null;
 }
}
