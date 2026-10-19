import { bootstrapApplication } from '@angular/platform-browser';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { IpliThemeService } from '@ipli/ui';

type Page='home'|'services'|'shop'|'about'|'contact';
@Component({
 selector:'app-root',standalone:true,
 imports:[MatButtonModule,MatIconModule,MatCardModule,MatButtonToggleModule],
 template:`
 <div class="site min-h-screen">
   <div class="bg-slate-950 text-center py-2.5 px-4 text-xs font-medium tracking-wide text-white">
     Welcome to our website. Contact us to learn more about our services.
   </div>
   <header class="site-header sticky top-0 z-20 bg-white/95 backdrop-blur-xl border-b border-slate-100">
     <div class="mx-auto max-w-7xl px-5 md:px-10 h-20 flex items-center gap-6">
       <button class="brand flex items-center gap-3" (click)="navigate('home')" aria-label="Open home page">
         <span class="brand-mark">S</span><span class="text-xl font-bold tracking-tight">STUDIO<span class="text-amber-600">.</span></span>
       </button>
       <nav class="hidden md:flex items-center gap-8 ml-auto" aria-label="Main navigation">
         @for(item of nav;track item.key){<button (click)="navigate(item.key)" [class.active-link]="page()===item.key" class="nav-link text-sm font-medium">{{item.label}}</button>}
       </nav>
       <button mat-flat-button class="!rounded-full ml-auto md:ml-3" (click)="navigate('contact')">Contact us <mat-icon iconPositionEnd>arrow_outward</mat-icon></button>
       <button class="md:hidden p-2" (click)="menuOpen.set(!menuOpen())" aria-label="Toggle menu"><mat-icon>menu</mat-icon></button>
     </div>
     @if(menuOpen()){<nav class="md:hidden px-6 pb-5 grid gap-4 bg-white" aria-label="Mobile navigation">
       @for(item of nav;track item.key){<button class="text-left py-2" (click)="navigate(item.key)">{{item.label}}</button>}
     </nav>}
   </header>
   <main id="main-content">
   @if(page()==='home'){
     <section class="hero relative overflow-hidden">
       <div class="max-w-7xl mx-auto grid lg:grid-cols-2 gap-12 items-center px-6 md:px-12 py-16 lg:py-24">
         <div class="relative z-10">
           <p class="eyebrow mb-5">YOUR BUSINESS. YOUR STORY.</p>
           <h1 class="hero-title">Good things<br/>start <span class="serif-accent">here.</span></h1>
           <p class="mt-7 text-lg leading-8 max-w-lg text-slate-600">Welcome to a space built for your customers. Show your services, share your work, and help people get in touch.</p>
           <div class="mt-10 flex gap-4 flex-wrap">
             <button mat-flat-button class="!rounded-full !px-8" (click)="navigate('services')">Explore services <mat-icon iconPositionEnd>arrow_forward</mat-icon></button>
             <button mat-stroked-button class="!rounded-full !px-8" (click)="navigate('about')">Our story</button>
           </div>
           <div class="mt-14 flex items-center gap-8 border-t border-slate-200 pt-6">
             <div><span class="block text-xl font-bold">01</span><span class="text-xs text-slate-500">Your own design</span></div>
             <div><span class="block text-xl font-bold">02</span><span class="text-xs text-slate-500">Your own content</span></div>
             <div><span class="block text-xl font-bold">03</span><span class="text-xs text-slate-500">Room to grow</span></div>
           </div>
         </div>
         <div class="relative hero-visual min-h-[430px] lg:min-h-[570px] rounded-[2rem] overflow-hidden">
           <div class="absolute inset-0 visual-gradient"></div>
           <div class="absolute left-10 top-12 text-white/80 text-xs tracking-[.3em] uppercase">Your brand space</div>
           <div class="absolute right-9 top-9 w-24 h-24 rounded-full border border-white/50"></div>
           <div class="absolute -left-12 bottom-16 w-44 h-44 rounded-full border border-white/35"></div>
           <div class="absolute bottom-12 left-10 right-10 text-white"><div class="text-7xl md:text-8xl font-light italic" style="font-family:Georgia,serif">Create.</div><div class="w-full h-px bg-white/40 my-4"></div><p class="text-sm tracking-widest uppercase">Make it yours</p></div>
         </div>
       </div>
     </section>
     <section class="max-w-7xl mx-auto px-6 md:px-12 py-24">
       <div class="flex items-end justify-between gap-5 flex-wrap mb-12">
         <div><p class="eyebrow">WHAT WE OFFER</p><h2 class="section-title mt-4">Discover our <i>services</i></h2></div>
         <button mat-stroked-button class="!rounded-full" (click)="navigate('services')">View all services <mat-icon iconPositionEnd>arrow_forward</mat-icon></button>
       </div>
       <div class="grid md:grid-cols-3 gap-5">
         @for(service of services;track service.number){
           <article class="service-card p-8 rounded-3xl">
             <span class="text-xs tracking-widest font-semibold text-amber-700">{{service.number}}</span>
             <div class="my-12 text-5xl">{{service.symbol}}</div>
             <h3 class="text-2xl font-semibold mb-3">{{service.title}}</h3>
             <p class="text-slate-600 leading-7">{{service.description}}</p>
             <button class="mt-8 inline-flex items-center gap-2 text-sm font-semibold" (click)="navigate('contact')">Ask about this service <mat-icon>arrow_outward</mat-icon></button>
           </article>
         }
       </div>
     </section>
     <section class="statement text-center px-6 py-28"><p class="eyebrow">THE NEXT STEP</p><h2 class="section-title mx-auto max-w-3xl mt-6">Let's build something <i>meaningful.</i></h2><p class="mt-7 text-slate-600">Tell us what you need. We will help you find the next step.</p><button mat-flat-button class="!rounded-full !mt-8" (click)="navigate('contact')">Get in touch</button></section>
   } @else if(page()==='services'){
     <section class="inner-hero"><p class="eyebrow">OUR SERVICES</p><h1 class="section-title mt-5">What we <i>offer.</i></h1><p class="mt-6 text-slate-600 max-w-xl mx-auto">Explore our services. Contact us for details and available times.</p></section>
     <section class="max-w-6xl mx-auto px-6 py-20 grid md:grid-cols-3 gap-6">
       @for(service of services;track service.number){<mat-card class="!rounded-3xl !p-7"><p class="text-5xl py-8">{{service.symbol}}</p><h2 class="text-2xl font-semibold">{{service.title}}</h2><p class="py-5 text-slate-600">{{service.description}}</p><button mat-stroked-button (click)="navigate('contact')">Contact us</button></mat-card>}
     </section>
   } @else if(page()==='shop'){
     <section class="inner-hero"><p class="eyebrow">SHOP</p><h1 class="section-title mt-5">Our <i>collection.</i></h1><p class="mt-6 text-slate-600 max-w-xl mx-auto">The shop is not open yet. Visit this page again after the store owner adds products.</p><button mat-flat-button class="!mt-8" (click)="navigate('contact')">Contact us</button></section>
   } @else if(page()==='about'){
     <section class="inner-hero"><p class="eyebrow">OUR STORY</p><h1 class="section-title mt-5">People come <i>first.</i></h1><p class="mt-6 text-slate-600 max-w-xl mx-auto">This website is ready for your company's story. Add your own text and media when the content editor is connected.</p></section>
   } @else {
     <section class="inner-hero"><p class="eyebrow">CONTACT</p><h1 class="section-title mt-5">Say <i>hello.</i></h1><p class="mt-6 text-slate-600 max-w-xl mx-auto">We would like to hear from you. Contact details will appear here after the business profile is connected.</p></section>
   }
   </main>
   <footer class="bg-slate-950 text-white px-6 md:px-12 py-16">
     <div class="max-w-7xl mx-auto grid md:grid-cols-3 gap-10">
       <div><div class="text-2xl font-bold">STUDIO<span class="text-amber-400">.</span></div><p class="mt-4 text-slate-400 max-w-sm leading-7">Your place for services, products, and helpful information.</p></div>
       <div><p class="font-semibold mb-5">Explore</p><div class="grid gap-3">@for(item of nav;track item.key){<button class="text-left text-slate-400 hover:text-white" (click)="navigate(item.key)">{{item.label}}</button>}</div></div>
       <div><p class="font-semibold mb-5">Appearance</p><mat-button-toggle-group [value]="theme.current()" (change)="theme.setTheme($event.value)" aria-label="Storefront theme"><mat-button-toggle value="light">Light</mat-button-toggle><mat-button-toggle value="dark">Dark</mat-button-toggle><mat-button-toggle value="ocean">Ocean</mat-button-toggle></mat-button-toggle-group><p class="mt-5 text-sm text-slate-400">Theme preview. The business owner will select the published theme.</p></div>
     </div><div class="max-w-7xl mx-auto border-t border-slate-700 mt-16 pt-6 text-sm text-slate-400">© {{year}} STUDIO. Example content. Replace it before publication.</div>
   </footer>
 </div>`
})
class StorefrontApp {
 readonly theme=inject(IpliThemeService);
 readonly page=signal<Page>('home');readonly menuOpen=signal(false);
 readonly year=new Date().getFullYear();
 readonly nav:{key:Page;label:string}[]=[{key:'home',label:'Home'},{key:'services',label:'Services'},{key:'shop',label:'Shop'},{key:'about',label:'About'},{key:'contact',label:'Contact'}];
 readonly services=[{number:'01',symbol:'✦',title:'Personal service',description:'Make each visit feel special. Tell your customers what makes your service unique.'},{number:'02',symbol:'◈',title:'Care and detail',description:'Show your experience and explain how you work.'},{number:'03',symbol:'✳',title:'Time for you',description:'Help customers learn about your business and contact your team.'}];
 constructor(){this.theme.load('storefront');}
 navigate(page:Page){this.page.set(page);this.menuOpen.set(false);window.scrollTo({top:0,behavior:'smooth'});}
}
bootstrapApplication(StorefrontApp).catch(console.error);
