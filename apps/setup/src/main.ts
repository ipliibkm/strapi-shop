import {bootstrapApplication} from '@angular/platform-browser';
import {Component, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {HttpClient, provideHttpClient} from '@angular/common/http';
import {firstValueFrom} from 'rxjs';
import {MatStepperModule} from '@angular/material/stepper';
import {MatInputModule} from '@angular/material/input';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatButtonModule} from '@angular/material/button';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatSelectModule} from '@angular/material/select';
import {MatCardModule} from '@angular/material/card';
import {IpliShellComponent} from '@ipli/ui';

@Component({selector:'app-root',standalone:true,
 imports:[FormsModule,MatStepperModule,MatInputModule,MatFormFieldModule,MatButtonModule,MatCheckboxModule,MatSelectModule,MatCardModule,IpliShellComponent],
 template:`
 <ipli-shell appName="Business setup" subtitle="First installation">
 <mat-card class="mx-auto max-w-3xl p-6"><h1 class="text-2xl font-semibold">Set up your business</h1>
 <p class="my-3">Complete each step. You can check the settings before you save them.</p>
 @if(message()){<p class="my-4 border rounded p-3" role="status">{{message()}}</p>}
 <mat-stepper linear #wizard>
 <mat-step [completed]="validCompany()"><ng-template matStepLabel>Company</ng-template>
 <div class="grid gap-3 mt-5 md:grid-cols-2">
 <mat-form-field><mat-label>Company name</mat-label><input matInput [(ngModel)]="company.name"></mat-form-field>
 <mat-form-field><mat-label>Contact email</mat-label><input matInput type="email" [(ngModel)]="company.contactEmail"></mat-form-field>
 <mat-form-field><mat-label>Time zone</mat-label><input matInput [(ngModel)]="company.timezone"></mat-form-field>
 <mat-form-field><mat-label>Language code</mat-label><input matInput [(ngModel)]="company.locale"></mat-form-field>
 <mat-form-field><mat-label>Currency</mat-label><input matInput [(ngModel)]="company.currency"></mat-form-field>
 <mat-form-field><mat-label>Address (optional)</mat-label><input matInput [(ngModel)]="company.address"></mat-form-field>
 <mat-form-field><mat-label>Phone (optional)</mat-label><input matInput [(ngModel)]="company.phone"></mat-form-field>
 </div><button mat-flat-button matStepperNext [disabled]="!validCompany()">Next</button>
 </mat-step>
 <mat-step [completed]="validAdmin()"><ng-template matStepLabel>Administrator</ng-template>
 <p class="my-4">Create the first operator record. Sign-in is not available yet.</p>
 <mat-form-field class="w-full"><mat-label>Admin email</mat-label><input matInput type="email" [(ngModel)]="admin.email"></mat-form-field>
 <mat-form-field class="w-full"><mat-label>Password (14 or more characters)</mat-label><input matInput type="password" autocomplete="new-password" [(ngModel)]="admin.password"></mat-form-field>
 <button mat-button matStepperPrevious>Back</button><button mat-flat-button matStepperNext [disabled]="!validAdmin()">Next</button>
 </mat-step>
 <mat-step><ng-template matStepLabel>Modules</ng-template>
 <p class="my-3">The website module is required. Other modules remain inactive until you configure them.</p>
 <div class="grid gap-2">@for(mod of optionalModules;track mod){<mat-checkbox [checked]="modules.includes(mod)" (change)="toggle(mod,$event.checked)">{{mod}}</mat-checkbox>}</div>
 <button mat-button matStepperPrevious>Back</button><button mat-flat-button matStepperNext>Next</button>
 </mat-step>
 <mat-step><ng-template matStepLabel>Branding</ng-template>
 <p class="my-3">Choose independent themes for the admin and the storefront.</p>
 <mat-form-field><mat-label>Admin theme</mat-label><mat-select [(ngModel)]="adminTheme">@for(theme of themes;track theme){<mat-option [value]="theme">{{theme}}</mat-option>}</mat-select></mat-form-field>
 <mat-form-field><mat-label>Storefront theme</mat-label><mat-select [(ngModel)]="storefrontTheme">@for(theme of themes;track theme){<mat-option [value]="theme">{{theme}}</mat-option>}</mat-select></mat-form-field>
 <button mat-button matStepperPrevious>Back</button><button mat-flat-button matStepperNext>Next</button>
 </mat-step>
 <mat-step><ng-template matStepLabel>Integrations</ng-template>
 <p class="my-3">Google Calendar, Gmail, and Stripe need account connections. Do not enter API secrets here.</p>
 <p>The setup saves module choices. It does not connect external services.</p>
 <button mat-button matStepperPrevious>Back</button><button mat-flat-button matStepperNext>Next</button>
 </mat-step>
 <mat-step><ng-template matStepLabel>Verify and save</ng-template>
 <p class="my-3">Company: {{company.name}}. Modules: {{modules.join(', ')}}.</p>
 <p class="my-2">Run the local setup script to get the setup token. Keep the token private.</p>
 <mat-form-field class="w-full"><mat-label>One-time setup token</mat-label><input matInput type="password" autocomplete="off" [(ngModel)]="token"></mat-form-field>
 <button mat-button matStepperPrevious>Back</button>
 <button mat-flat-button [disabled]="saving()||!validCompany()||!validAdmin()||token.length<32" (click)="save()">Save setup</button>
 </mat-step></mat-stepper></mat-card></ipli-shell>
 `})
class SetupApp {
 private http=inject(HttpClient);
 readonly themes=['light','dark','ocean']; readonly optionalModules=['commerce','booking','calendar','gmail'];
 company={name:'',contactEmail:'',timezone:'Europe/Berlin',locale:'de',currency:'EUR',address:'',phone:''};
 admin={email:'',password:''};
 modules=['website'];adminTheme='light';storefrontTheme='light';token='';
 message=signal('');saving=signal(false);
 validCompany(){return !!this.company.name.trim()&&this.company.contactEmail.includes('@')&&!!this.company.timezone.trim()&&this.company.currency.length===3;}
 validAdmin(){return this.admin.email.includes('@')&&this.admin.password.length>=14;}
 toggle(module:string,checked:boolean){this.modules=checked?[...this.modules,module]:this.modules.filter(x=>x!==module);}
 async save(){this.saving.set(true);this.message.set('Save the setup settings.');
  try{const r=await firstValueFrom(this.http.post<{message:string}>('/setup/api/complete',
    {company:this.company,admin:this.admin,modules:this.modules,adminTheme:this.adminTheme,storefrontTheme:this.storefrontTheme},
    {headers:{'X-Setup-Token':this.token}}));
    this.message.set(r.message);this.admin.password='';this.token='';
  }catch(e:any){this.message.set(e?.error?.message||'The server did not save the settings. Check the token and try again.');}
  finally{this.saving.set(false);}
 }
}
bootstrapApplication(SetupApp,{providers:[provideHttpClient()]}).catch(console.error);
