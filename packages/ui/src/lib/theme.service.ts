import { DOCUMENT } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';
export type IpliTheme = 'light'|'dark'|'ocean';
const presets:Record<IpliTheme,{bg:string,fg:string,accent:string}> = {light:{bg:'#f7f8fc',fg:'#1e293b',accent:'#3f51b5'},dark:{bg:'#111827',fg:'#e5e7eb',accent:'#818cf8'},ocean:{bg:'#e9f7f7',fg:'#0c3742',accent:'#007c91'}};
@Injectable({providedIn:'root'})
export class IpliThemeService {
 private doc=inject(DOCUMENT);
 readonly current=signal<IpliTheme>('light');
 private scope='admin';
 load(scope:'admin'|'storefront') { this.scope=scope; const saved=this.doc.defaultView?.localStorage.getItem('ipli-theme-'+scope); this.setTheme(saved && saved in presets ? saved as IpliTheme:'light'); }
 setTheme(theme:IpliTheme) {const palette=presets[theme];this.current.set(theme);const root=this.doc.documentElement;root.style.setProperty('--ipli-bg',palette.bg);root.style.setProperty('--ipli-fg',palette.fg);root.style.setProperty('--ipli-accent',palette.accent);this.doc.defaultView?.localStorage.setItem('ipli-theme-'+this.scope,theme);}
}
