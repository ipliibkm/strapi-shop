/** Admin panel interface translations for the supported languages. */
export type Lang = 'de' | 'en' | 'ru' | 'tr';

export const LANGUAGES: Record<Lang, { code: Lang; nativeName: string }> = {
  de: { code: 'de', nativeName: 'Deutsch' },
  en: { code: 'en', nativeName: 'English' },
  ru: { code: 'ru', nativeName: 'Русский' },
  tr: { code: 'tr', nativeName: 'Türkçe' },
};

/** BCP-47 locales used for dates and prices in each language. */
const LOCALES: Record<Lang, string> = { de: 'de-DE', en: 'en', ru: 'ru-RU', tr: 'tr-TR' };

export function localeOf(lang: Lang): string {
  return LOCALES[lang];
}

type Dict = Record<string, string>;

const de: Dict = {};
const ru: Dict = {};
const tr: Dict = {};

/** English is the source language and needs no dictionary. */
const dictionaries: Record<Lang, Dict> = { en: {}, de, ru, tr };

/**
 * Translates one admin interface string for the given language.
 * Placeholders like `{count}` are replaced with the provided values.
 */
export function translate(
  lang: Lang,
  text: string,
  params?: Record<string, string | number>,
): string {
  let result = dictionaries[lang][text] ?? text;
  if (params) {
    for (const [key, value] of Object.entries(params)) {
      result = result.replaceAll(`{${key}}`, String(value));
    }
  }
  return result;
}
de['(empty)'] = '(leer)';
ru['(empty)'] = '(пусто)';
tr['(empty)'] = '(boş)';
de['No email provided'] = 'Keine E-Mail angegeben';
ru['No email provided'] = 'Электронная почта не указана';
tr['No email provided'] = 'E-posta belirtilmedi';
de['{amount} refunded'] = '{amount} erstattet';
ru['{amount} refunded'] = 'возвращено {amount}';
tr['{amount} refunded'] = '{amount} iade edildi';
de['{count} active'] = '{count} aktiv';
ru['{count} active'] = 'активно: {count}';
tr['{count} active'] = '{count} aktif';
de['{count} cancelled'] = '{count} storniert';
ru['{count} cancelled'] = 'отменено: {count}';
tr['{count} cancelled'] = '{count} iptal';
de['{count} confirmed bookings in total'] = 'insgesamt {count} bestätigte Buchungen';
ru['{count} confirmed bookings in total'] = 'всего подтверждённых бронирований: {count}';
tr['{count} confirmed bookings in total'] = 'toplam {count} onaylı rezervasyon';
de['{count} confirmed'] = '{count} bestätigt';
ru['{count} confirmed'] = 'подтверждено: {count}';
tr['{count} confirmed'] = '{count} onaylı';
de['{count} fields'] = '{count} Felder';
ru['{count} fields'] = 'полей: {count}';
tr['{count} fields'] = '{count} alan';
de['{count} min'] = '{count} Min.';
ru['{count} min'] = '{count} мин';
tr['{count} min'] = '{count} dk';
de['{count} option(s)'] = '{count} Option(en)';
ru['{count} option(s)'] = 'вариантов: {count}';
tr['{count} option(s)'] = '{count} seçenek';
de['{count} OPTION(S)'] = '{count} OPTION(EN)';
ru['{count} OPTION(S)'] = 'ВАРИАНТОВ: {count}';
tr['{count} OPTION(S)'] = '{count} SEÇENEK';
de['{count} paid, {cancelled} cancelled'] = '{count} bezahlt, {cancelled} storniert';
ru['{count} paid, {cancelled} cancelled'] = 'оплачено: {count}, отменено: {cancelled}';
tr['{count} paid, {cancelled} cancelled'] = '{count} ödenmiş, {cancelled} iptal';
de['{count} submissions across all forms'] = '{count} Einsendungen über alle Formulare';
ru['{count} submissions across all forms'] = 'отправок по всем формам: {count}';
tr['{count} submissions across all forms'] = 'tüm formlarda {count} gönderim';
de['{count} synchronized with a calendar'] = '{count} mit einem Kalender synchronisiert';
ru['{count} synchronized with a calendar'] = 'синхронизировано с календарём: {count}';
tr['{count} synchronized with a calendar'] = 'takvimle senkronize edilen: {count}';
de['{count} units sold'] = '{count} verkaufte Einheiten';
ru['{count} units sold'] = 'продано единиц: {count}';
tr['{count} units sold'] = 'satılan adet: {count}';
de['{count} with customer details'] = '{count} mit Kundendaten';
ru['{count} with customer details'] = 'с данными клиента: {count}';
tr['{count} with customer details'] = 'müşteri bilgisi içeren: {count}';
de['About Us'] = 'Über uns';
ru['About Us'] = 'О нас';
tr['About Us'] = 'Hakkımızda';
de['Activate'] = 'Aktivieren';
ru['Activate'] = 'Активировать';
tr['Activate'] = 'Etkinleştir';
de['Active modules'] = 'Aktive Module';
ru['Active modules'] = 'Активные модули';
tr['Active modules'] = 'Aktif modüller';
de['Add a booking service'] = 'Buchungsdienst hinzufügen';
ru['Add a booking service'] = 'Добавить услугу бронирования';
tr['Add a booking service'] = 'Rezervasyon hizmeti ekle';
de['Add a new section:'] = 'Neue Sektion hinzufügen:';
ru['Add a new section:'] = 'Добавить новый раздел:';
tr['Add a new section:'] = 'Yeni bölüm ekle:';
de['Add at least one purchasable option.'] = 'Fügen Sie mindestens eine kaufbare Option hinzu.';
ru['Add at least one purchasable option.'] = 'Добавьте хотя бы один покупаемый вариант.';
tr['Add at least one purchasable option.'] = 'En az bir satın alınabilir seçenek ekleyin.';
de['Add field'] = 'Feld hinzufügen';
ru['Add field'] = 'Добавить поле';
tr['Add field'] = 'Alan ekle';
de['Add fields and validation rules. You can then add the form to a storefront page.'] =
  'Felder und Validierungsregeln hinzufügen. Danach können Sie das Formular einer Storefront-Seite zuordnen.';
ru['Add fields and validation rules. You can then add the form to a storefront page.'] =
  'Добавьте поля и правила проверки. Затем вы сможете добавить форму на страницу витрины.';
tr['Add fields and validation rules. You can then add the form to a storefront page.'] =
  'Alan ve doğrulama kuralları ekleyin. Ardından formu vitrin sayfasına ekleyebilirsiniz.';
de['Add option'] = 'Option hinzufügen';
ru['Add option'] = 'Добавить вариант';
tr['Add option'] = 'Seçenek ekle';
de['Add service'] = 'Dienst hinzufügen';
ru['Add service'] = 'Добавить услугу';
tr['Add service'] = 'Hizmet ekle';
de['Address'] = 'Adresse';
ru['Address'] = 'Адрес';
tr['Address'] = 'Adres';
de['Admin panel theme'] = 'Adminpanel-Thema';
ru['Admin panel theme'] = 'Тема панели администратора';
tr['Admin panel theme'] = 'Yönetici paneli teması';
de['Admin sign-in'] = 'Admin-Anmeldung';
ru['Admin sign-in'] = 'Вход администратора';
tr['Admin sign-in'] = 'Yönetici girişi';
de['Admin theme'] = 'Admin-Thema';
ru['Admin theme'] = 'Тема администратора';
tr['Admin theme'] = 'Yönetici teması';
de['Administration tools'] = 'Verwaltungswerkzeuge';
ru['Administration tools'] = 'Инструменты управления';
tr['Administration tools'] = 'Yönetim araçları';
de['Administrator sign-in is not available. Try again later.'] =
  'Die Admin-Anmeldung ist nicht verfügbar. Versuchen Sie es später erneut.';
ru['Administrator sign-in is not available. Try again later.'] =
  'Вход администратора недоступен. Повторите попытку позже.';
tr['Administrator sign-in is not available. Try again later.'] =
  'Yönetici girişi kullanılamıyor. Daha sonra tekrar deneyin.';
de['All confirmed bookings'] = 'Alle bestätigten Buchungen';
ru['All confirmed bookings'] = 'Все подтверждённые бронирования';
tr['All confirmed bookings'] = 'Tüm onaylı rezervasyonlar';
de['All statuses'] = 'Alle Status';
ru['All statuses'] = 'Все статусы';
tr['All statuses'] = 'Tüm durumlar';
de['All storefront forms'] = 'Alle Storefront-Formulare';
ru['All storefront forms'] = 'Все формы витрины';
tr['All storefront forms'] = 'Tüm vitrin formları';
de['Appearance and languages'] = 'Erscheinungsbild und Sprachen';
ru['Appearance and languages'] = 'Оформление и языки';
tr['Appearance and languages'] = 'Görünüm ve diller';
de['Article content editor'] = 'Artikel-Texteditor';
ru['Article content editor'] = 'Редактор текста статьи';
tr['Article content editor'] = 'Makale içerik düzenleyicisi';
de['Article list'] = 'Artikelliste';
ru['Article list'] = 'Список статей';
tr['Article list'] = 'Makale listesi';
de['Article title'] = 'Artikeltitel';
ru['Article title'] = 'Заголовок статьи';
tr['Article title'] = 'Makale başlığı';
de['Ask a Strapi Super Admin to change your Content Manager permissions.'] =
  'Bitten Sie einen Strapi-Superadmin, Ihre Berechtigungen im Content Manager zu ändern.';
ru['Ask a Strapi Super Admin to change your Content Manager permissions.'] =
  'Попросите супер-администратора Strapi изменить ваши права в Content Manager.';
tr['Ask a Strapi Super Admin to change your Content Manager permissions.'] =
  'Content Manager izinlerinizi değiştirmesi için bir Strapi Süper Yöneticisine başvurun.';
de['Automatic token refresh is {state}. Current token expires at {time}.'] =
  'Automatisches Token-Refresh ist {state}. Das aktuelle Token läuft um {time} ab.';
ru['Automatic token refresh is {state}. Current token expires at {time}.'] =
  'Автоматическое обновление токена: {state}. Текущий токен истекает в {time}.';
tr['Automatic token refresh is {state}. Current token expires at {time}.'] =
  'Otomatik token yenileme {state}. Geçerli token {time} tarihinde doluyor.';
de['Availability and confirmed bookings'] = 'Verfügbarkeit und bestätigte Buchungen';
ru['Availability and confirmed bookings'] = 'Доступность и подтверждённые бронирования';
tr['Availability and confirmed bookings'] = 'Müsaitlik ve onaylı rezervasyonlar';
de['available'] = 'verfügbar';
ru['available'] = 'доступно';
tr['available'] = 'mevcut';
de['Available'] = 'Verfügbar';
ru['Available'] = 'Доступен';
tr['Available'] = 'Mevcut';
de['AVAILABLE'] = 'VERFÜGBAR';
ru['AVAILABLE'] = 'ДОСТУПНО';
tr['AVAILABLE'] = 'KULLANILABİLİR';
de['Backend online'] = 'Backend online';
ru['Backend online'] = 'Бэкенд в сети';
tr['Backend online'] = 'Arka uç çevrimiçi';
de['Backend unavailable'] = 'Backend nicht verfügbar';
ru['Backend unavailable'] = 'Бэкенд недоступен';
tr['Backend unavailable'] = 'Arka uç kullanılamıyor';
de['Bold'] = 'Fett';
ru['Bold'] = 'Жирный';
tr['Bold'] = 'Kalın';
de['Book a Visit'] = 'Besuch buchen';
ru['Book a Visit'] = 'Записаться на визит';
tr['Book a Visit'] = 'Ziyaret randevusu al';
de['Booking API'] = 'Buchungs-API';
ru['Booking API'] = 'API бронирования';
tr['Booking API'] = 'Rezervasyon API';
de['Booking calendar'] = 'Buchungskalender';
ru['Booking calendar'] = 'Календарь бронирований';
tr['Booking calendar'] = 'Rezervasyon takvimi';
de['BOOKING DETAILS'] = 'BUCHUNGSDETAILS';
ru['BOOKING DETAILS'] = 'ДЕТАЛИ БРОНИРОВАНИЯ';
tr['BOOKING DETAILS'] = 'REZERVASYON DETAYLARI';
de['Booking operations'] = 'Buchungsbetrieb';
ru['Booking operations'] = 'Операции бронирования';
tr['Booking operations'] = 'Rezervasyon işlemleri';
de['BOOKING SETUP'] = 'BUCHUNGSEINSTELLUNG';
ru['BOOKING SETUP'] = 'НАСТРОЙКА БРОНИРОВАНИЯ';
tr['BOOKING SETUP'] = 'REZERVASYON AYARLARI';
de['Bookings'] = 'Buchungen';
ru['Bookings'] = 'Бронирования';
tr['Bookings'] = 'Rezervasyonlar';
de['BOOKINGS'] = 'BUCHUNGEN';
ru['BOOKINGS'] = 'БРОНИРОВАНИЯ';
tr['BOOKINGS'] = 'REZERVASYONLAR';
de['Builder'] = 'Builder';
ru['Builder'] = 'Конструктор';
tr['Builder'] = 'Oluşturucu';
de['Bulleted list'] = 'Aufzählungsliste';
ru['Bulleted list'] = 'Маркированный список';
tr['Bulleted list'] = 'Noktalı liste';
de['Business at a glance.'] = 'Geschäft auf einen Blick.';
ru['Business at a glance.'] = 'Бизнес в одном взгляде.';
tr['Business at a glance.'] = 'İşinize genel bakış.';
de['BUSINESS FIGURES'] = 'GESCHÄFTSZAHLEN';
ru['BUSINESS FIGURES'] = 'БИЗНЕС-ПОКАЗАТЕЛИ';
tr['BUSINESS FIGURES'] = 'İŞ RAKAMLARI';
de['BUSINESS OVERVIEW'] = 'GESCHÄFTSÜBERSICHT';
ru['BUSINESS OVERVIEW'] = 'ОБЗОР БИЗНЕСА';
tr['BUSINESS OVERVIEW'] = 'İŞ GENEL BAKIŞI';
de['Business Profile is available'] = 'Business-Profil ist verfügbar';
ru['Business Profile is available'] = 'Профиль бизнеса доступен';
tr['Business Profile is available'] = 'İş profili mevcut';
de['Business Profile'] = 'Business-Profil';
ru['Business Profile'] = 'Профиль бизнеса';
tr['Business Profile'] = 'İş profili';
de['Business summary'] = 'Geschäftsübersicht';
ru['Business summary'] = 'Сводка по бизнесу';
tr['Business summary'] = 'İş özeti';
de['Business tools'] = 'Geschäftswerkzeuge';
ru['Business tools'] = 'Бизнес-инструменты';
tr['Business tools'] = 'İş araçları';
de['Button / CTA'] = 'Button / CTA';
ru['Button / CTA'] = 'Кнопка / CTA';
tr['Button / CTA'] = 'Buton / CTA';
de['Button text'] = 'Button-Text';
ru['Button text'] = 'Текст кнопки';
tr['Button text'] = 'Buton metni';
de['By units sold'] = 'Nach verkauften Einheiten';
ru['By units sold'] = 'По проданным единицам';
tr['By units sold'] = 'Satılan adete göre';
de['Calendar status'] = 'Kalenderstatus';
ru['Calendar status'] = 'Статус календаря';
tr['Calendar status'] = 'Takvim durumu';
de['Calendar synchronization failed. Check the mapping status.'] =
  'Kalendersynchronisierung fehlgeschlagen. Prüfen Sie den Zuordnungsstatus.';
ru['Calendar synchronization failed. Check the mapping status.'] =
  'Синхронизация календаря не удалась. Проверьте статус сопоставления.';
tr['Calendar synchronization failed. Check the mapping status.'] =
  'Takvim senkronizasyonu başarısız oldu. Eşleme durumunu kontrol edin.';
de['Calendar synchronization finished.'] = 'Kalendersynchronisierung abgeschlossen.';
ru['Calendar synchronization finished.'] = 'Синхронизация календаря завершена.';
tr['Calendar synchronization finished.'] = 'Takvim senkronizasyonu tamamlandı.';
de['Call-to-Action Button'] = 'Call-to-Action-Button';
ru['Call-to-Action Button'] = 'Кнопка призыва к действию';
tr['Call-to-Action Button'] = 'Eylem çağrısı butonu';
de['Cancel booking'] = 'Buchung stornieren';
ru['Cancel booking'] = 'Отменить бронирование';
tr['Cancel booking'] = 'Rezervasyonu iptal et';
de['Cancel order'] = 'Bestellung stornieren';
ru['Cancel order'] = 'Отменить заказ';
tr['Cancel order'] = 'Siparişi iptal et';
de['Cancel the booking for {name}?'] = 'Die Buchung für {name} stornieren?';
ru['Cancel the booking for {name}?'] = 'Отменить бронирование для {name}?';
tr['Cancel the booking for {name}?'] = '{name} için rezervasyonu iptal edilsin mi?';
de['Cancel'] = 'Abbrechen';
ru['Cancel'] = 'Отмена';
tr['Cancel'] = 'İptal';
de['Cancelled'] = 'Storniert';
ru['Cancelled'] = 'Отменено';
tr['Cancelled'] = 'İptal edildi';
de['CANCELLED'] = 'STORNIERT';
ru['CANCELLED'] = 'ОТМЕНЕНО';
tr['CANCELLED'] = 'İPTAL';
de['Capability'] = 'Funktion';
ru['Capability'] = 'Возможность';
tr['Capability'] = 'Özellik';
de['Caption below image (optional)'] = 'Bildunterschrift (optional)';
ru['Caption below image (optional)'] = 'Подпись под изображением (необязательно)';
tr['Caption below image (optional)'] = 'Görselin altındaki açıklama (isteğe bağlı)';
de['Carrier'] = 'Spediteur';
ru['Carrier'] = 'Перевозчик';
tr['Carrier'] = 'Taşıyıcı';
de['CATALOG'] = 'KATALOG';
ru['CATALOG'] = 'КАТАЛОГ';
tr['CATALOG'] = 'KATALOG';
de['Check and reserve a known resource'] = 'Bekannte Ressource prüfen und reservieren';
ru['Check and reserve a known resource'] = 'Проверить и зарезервировать известный ресурс';
tr['Check and reserve a known resource'] = 'Bilinen bir kaynağı kontrol edip ayırın';
de['check the backend logs.'] = 'Prüfen Sie die Backend-Logs.';
ru['check the backend logs.'] = 'проверьте журналы бэкенда.';
tr['check the backend logs.'] = 'arka uç günlüklerini kontrol edin.';
de['Check which services are configured and running on this installation.'] =
  'Prüfen Sie, welche Dienste auf dieser Installation konfiguriert und aktiv sind.';
ru['Check which services are configured and running on this installation.'] =
  'Проверьте, какие службы настроены и запущены в этой установке.';
tr['Check which services are configured and running on this installation.'] =
  'Bu kurulumda yapılandırılmış ve çalışan hizmetleri kontrol edin.';
de['Checkbox'] = 'Kästchen';
ru['Checkbox'] = 'Флажок';
tr['Checkbox'] = 'Onay kutusu';
de[
  'Choose the color palette and website languages for your customer site. Saved changes apply to every visitor after you save them.'
] =
  'Wählen Sie die Farbpalette und Website-Sprachen für Ihre Kundenseite. Gespeicherte Änderungen gelten nach dem Speichern für jeden Besucher.';
ru[
  'Choose the color palette and website languages for your customer site. Saved changes apply to every visitor after you save them.'
] =
  'Выберите цветовую палитру и языки сайта для клиентского сайта. Сохранённые изменения применяются ко всем посетителям после сохранения.';
tr[
  'Choose the color palette and website languages for your customer site. Saved changes apply to every visitor after you save them.'
] =
  'Müşteri siteniz için renk paletini ve site dillerini seçin. Kaydedilen değişiklikler kaydedildikten sonra tüm ziyaretçilere uygulanır.';
de['Close dialog'] = 'Dialog schließen';
ru['Close dialog'] = 'Закрыть диалог';
tr['Close dialog'] = 'Pencereyi kapat';
de['Close'] = 'Schließen';
ru['Close'] = 'Закрыть';
tr['Close'] = 'Kapat';
de['CLOSED'] = 'GESCHLOSSEN';
ru['CLOSED'] = 'ЗАКРЫТО';
tr['CLOSED'] = 'KAPALI';
de['CMS content models'] = 'CMS-Inhaltsmodelle';
ru['CMS content models'] = 'Модели контента CMS';
tr['CMS content models'] = 'CMS içerik modelleri';
de['COLOR PALETTE'] = 'FARBPALETTE';
ru['COLOR PALETTE'] = 'ЦВЕТОВАЯ ПАЛИТРА';
tr['COLOR PALETTE'] = 'RENK PALETİ';
de['Commerce operations'] = 'Commerce-Betrieb';
ru['Commerce operations'] = 'Операции торговли';
tr['Commerce operations'] = 'Ticaret işlemleri';
de['Configuration checks'] = 'Konfigurationsprüfungen';
ru['Configuration checks'] = 'Проверки конфигурации';
tr['Configuration checks'] = 'Yapılandırma denetimleri';
de['Configured services'] = 'Konfigurierte Dienste';
ru['Configured services'] = 'Настроенные службы';
tr['Configured services'] = 'Yapılandırılmış hizmetler';
de['confirmed bookings'] = 'bestätigte Buchungen';
ru['confirmed bookings'] = 'подтверждённых бронирований';
tr['confirmed bookings'] = 'onaylı rezervasyon';
de['Connect a resource'] = 'Ressource verbinden';
ru['Connect a resource'] = 'Подключить ресурс';
tr['Connect a resource'] = 'Kaynak bağla';
de['Connect Google only for Calendar and future Gmail services.'] =
  'Verbinden Sie Google nur für Kalender und zukünftige Gmail-Dienste.';
ru['Connect Google only for Calendar and future Gmail services.'] =
  'Подключайте Google только для Календаря и будущих сервисов Gmail.';
tr['Connect Google only for Calendar and future Gmail services.'] =
  'Googleı yalnızca Takvim ve gelecekteki Gmail hizmetleri için bağlayın.';
de['Connect Google services'] = 'Google-Dienste verbinden';
ru['Connect Google services'] = 'Подключить сервисы Google';
tr['Connect Google services'] = 'Google hizmetlerini bağla';
de['Connect'] = 'Verbinden';
ru['Connect'] = 'Подключить';
tr['Connect'] = 'Bağlan';
de['CONNECTED'] = 'VERBUNDEN';
ru['CONNECTED'] = 'ПОДКЛЮЧЕНО';
tr['CONNECTED'] = 'BAĞLI';
de['Contact email'] = 'Kontakt-E-Mail';
ru['Contact email'] = 'Контактный e-mail';
tr['Contact email'] = 'İletişim e-posta';
de['Contact'] = 'Kontakt';
ru['Contact'] = 'Контакты';
tr['Contact'] = 'İletişim';
de['Content maintenance'] = 'Inhaltspflege';
ru['Content maintenance'] = 'Обслуживание контента';
tr['Content maintenance'] = 'İçerik bakımı';
de['Content saved.'] = 'Inhalt gespeichert.';
ru['Content saved.'] = 'Контент сохранён.';
tr['Content saved.'] = 'İçerik kaydedildi.';
de['Content workspace'] = 'Inhaltsarbeitsbereich';
ru['Content workspace'] = 'Рабочая область контента';
tr['Content workspace'] = 'İçerik çalışma alanı';
de['CONTENT'] = 'INHALT';
ru['CONTENT'] = 'СОДЕРЖАНИЕ';
tr['CONTENT'] = 'İÇERİK';
de['Create a product'] = 'Produkt erstellen';
ru['Create a product'] = 'Создать товар';
tr['Create a product'] = 'Ürün oluştur';
de['Create a reusable form.'] = 'Erstellen Sie ein wiederverwendbares Formular.';
ru['Create a reusable form.'] = 'Создайте переиспользуемую форму.';
tr['Create a reusable form.'] = 'Yeniden kullanılabilir bir form oluşturun.';
de['Create and publish reusable forms in the Forms workspace.'] =
  'Erstellen und veröffentlichen Sie wiederverwendbare Formulare im Formular-Arbeitsbereich.';
ru['Create and publish reusable forms in the Forms workspace.'] =
  'Создавайте и публикуйте переиспользуемые формы в рабочей области форм.';
tr['Create and publish reusable forms in the Forms workspace.'] =
  'Formlar çalışma alanında yeniden kullanılabilir formlar oluşturun ve yayınlayın.';
de['Create form'] = 'Formular erstellen';
ru['Create form'] = 'Создать форму';
tr['Create form'] = 'Form oluştur';
de['Create product'] = 'Produkt anlegen';
ru['Create product'] = 'Создать товар';
tr['Create product'] = 'Ürün oluştur';
de['Create reusable forms and configure their fields and validation rules.'] =
  'Erstellen Sie wiederverwendbare Formulare und konfigurieren Sie deren Felder und Validierungsregeln.';
ru['Create reusable forms and configure their fields and validation rules.'] =
  'Создавайте переиспользуемые формы и настраивайте их поля и правила проверки.';
tr['Create reusable forms and configure their fields and validation rules.'] =
  'Yeniden kullanılabilir formlar oluşturun ve alanlarını ile doğrulama kurallarını yapılandırın.';
de['Currency'] = 'Währung';
ru['Currency'] = 'Валюта';
tr['Currency'] = 'Para birimi';
de['Customer booking options'] = 'Kundenbuchungsoptionen';
ru['Customer booking options'] = 'Варианты бронирования клиентов';
tr['Customer booking options'] = 'Müşteri rezervasyon seçenekleri';
de['CUSTOMER BOOKING'] = 'KUNDENBUCHUNG';
ru['CUSTOMER BOOKING'] = 'БРОНИРОВАНИЕ КЛИЕНТАМИ';
tr['CUSTOMER BOOKING'] = 'MÜŞTERİ REZERVASYONU';
de['CUSTOMER CATALOG'] = 'KUNDENKATALOG';
ru['CUSTOMER CATALOG'] = 'КАТАЛОГ КЛИЕНТОВ';
tr['CUSTOMER CATALOG'] = 'MÜŞTERİ KATALOĞU';
de['Customer not recorded'] = 'Kein Kunde erfasst';
ru['Customer not recorded'] = 'Клиент не указан';
tr['Customer not recorded'] = 'Müşteri kaydedilmemiş';
de['Customer orders'] = 'Kundenbestellungen';
ru['Customer orders'] = 'Заказы клиентов';
tr['Customer orders'] = 'Müşteri siparişleri';
de['Customer'] = 'Kunde';
ru['Customer'] = 'Клиент';
tr['Customer'] = 'Müşteri';
de['Customers use the default appointment duration while the catalog is empty.'] =
  'Solange der Katalog leer ist, nutzen Kunden die Standard-Terminsdauer.';
ru['Customers use the default appointment duration while the catalog is empty.'] =
  'Пока каталог пуст, клиенты используют стандартную длительность визита.';
tr['Customers use the default appointment duration while the catalog is empty.'] =
  'Katalog boşken müşteriler varsayılan randevu süresini kullanır.';
de['DAILY TREND'] = 'TÄGLICHER VERLAUF';
ru['DAILY TREND'] = 'ЕЖЕДНЕВНАЯ ДИНАМИКА';
tr['DAILY TREND'] = 'GÜNLÜK EĞİLİM';
de['DATA EXPORT'] = 'DATENEXPORT';
ru['DATA EXPORT'] = 'ЭКСПОРТ ДАННЫХ';
tr['DATA EXPORT'] = 'VERİ İHRACATI';
de['Date'] = 'Datum';
ru['Date'] = 'Дата';
tr['Date'] = 'Tarih';
de['Default duration'] = 'Standarddauer';
ru['Default duration'] = 'Длительность по умолчанию';
tr['Default duration'] = 'Varsayılan süre';
de['Define the services that customers can book and set the duration of each appointment.'] =
  'Definieren Sie die Dienste, die Kunden buchen können, und legen Sie die Dauer jedes Termins fest.';
ru['Define the services that customers can book and set the duration of each appointment.'] =
  'Определите услуги, которые могут бронировать клиенты, и задайте длительность каждого визита.';
tr['Define the services that customers can book and set the duration of each appointment.'] =
  'Müşterilerin rezervasyon yapabileceği hizmetleri tanımlayın ve her randevunun süresini belirleyin.';
de['Delete product'] = 'Produkt löschen';
ru['Delete product'] = 'Удалить товар';
tr['Delete product'] = 'Ürünü sil';
de['Delete submission'] = 'Einsendung löschen';
ru['Delete submission'] = 'Удалить отправку';
tr['Delete submission'] = 'Gönderimi sil';
de['Delete this submission? This action cannot be undone.'] =
  'Diese Einsendung löschen? Diese Aktion kann nicht rückgängig gemacht werden.';
ru['Delete this submission? This action cannot be undone.'] =
  'Удалить эту отправку? Это действие нельзя отменить.';
tr['Delete this submission? This action cannot be undone.'] =
  'Bu gönderim silinsin mi? Bu işlem geri alınamaz.';
de['Delete'] = 'Löschen';
ru['Delete'] = 'Удалить';
tr['Delete'] = 'Sil';
de['Delivery failed'] = 'Zustellung fehlgeschlagen';
ru['Delivery failed'] = 'Доставка не удалась';
tr['Delivery failed'] = 'Teslimat başarısız';
de['Delivery queued'] = 'Zustellung in Warteschlange';
ru['Delivery queued'] = 'Доставка в очереди';
tr['Delivery queued'] = 'Teslimat kuyrukta';
de['Delivery retry scheduled'] = 'Neuer Zustellversuch geplant';
ru['Delivery retry'] = 'Повторная доставка запланирована';
tr['Delivery retry scheduled'] = 'Teslimat yeniden denemesi planlandı';
de['Describe this photo...'] = 'Beschreiben Sie dieses Foto…';
ru['Describe this photo...'] = 'Опишите это фото…';
tr['Describe this photo...'] = 'Bu fotoğrafı açıklayın…';
de['Description'] = 'Beschreibung';
ru['Description'] = 'Описание';
tr['Description'] = 'Açıklama';
de['Image URL'] = 'Bild-Adresse';
ru['Image URL'] = 'Адрес изображения';
tr['Image URL'] = 'Görsel adresi';
de['Detailed numbers'] = 'Detailli Zahlen';
ru['Detailed numbers'] = 'Подробные показатели';
tr['Detailed numbers'] = 'Detaylı rakamlar';
de['disabled'] = 'deaktiviert';
ru['disabled'] = 'отключено';
tr['disabled'] = 'devre dışı';
de['Disabled'] = 'Deaktiviert';
ru['Disabled'] = 'Отключено';
tr['Disabled'] = 'Devre dışı';
de['Disconnect'] = 'Trennen';
ru['Disconnect'] = 'Отключить';
tr['Disconnect'] = 'Bağlantıyı kes';
de['Download CSV'] = 'CSV herunterladen';
ru['Download CSV'] = 'Скачать CSV';
tr['Download CSV'] = 'CSV indir';
de['Download order CSV'] = 'Bestell-CSV herunterladen';
ru['Download order CSV'] = 'Скачать CSV заказов';
tr['Download order CSV'] = 'Sipariş CSV indir';
de['Draft saved. The storefront still shows the previous version.'] =
  'Entwurf gespeichert. Die Storefront zeigt weiterhin die vorherige Version.';
ru['Draft saved. The storefront still shows the previous version.'] =
  'Черновик сохранён. Витрина по-прежнему показывает предыдущую версию.';
tr['Draft saved. The storefront still shows the previous version.'] =
  'Taslak kaydedildi. Vitrin hâlâ önceki sürümü gösteriyor.';
de['Draft saved.'] = 'Entwurf gespeichert.';
ru['Draft saved.'] = 'Черновик сохранён.';
tr['Draft saved.'] = 'Taslak kaydedildi.';
de['draft'] = 'Entwurf';
ru['draft'] = 'черновик';
tr['draft'] = 'taslak';
de['DRAFT'] = 'ENTWURF';
ru['DRAFT'] = 'ЧЕРНОВИК';
tr['DRAFT'] = 'TASLAK';
de['Dropdown options'] = 'Dropdown-Optionen';
ru['Dropdown options'] = 'Варианты выпадающего списка';
tr['Dropdown options'] = 'Açılır menü seçenekleri';
de['Dropdown'] = 'Dropdown';
ru['Dropdown'] = 'Выпадающий список';
tr['Dropdown'] = 'Açılır menü';
de['Duration'] = 'Dauer';
ru['Duration'] = 'Длительность';
tr['Duration'] = 'Süre';
de['e.g. 1234567890'] = 'z. B. 1234567890';
ru['e.g. 1234567890'] = 'напр., 1234567890';
tr['e.g. 1234567890'] = 'örn. 1234567890';
de['e.g., Book a visit'] = 'z. B. Besuch buchen';
ru['e.g., Book a visit'] = 'напр., Записаться на визит';
tr['e.g., Book a visit'] = 'örn. Ziyaret randevusu al';
de['e.g., Browse our collection'] = 'z. B. Unsere Kollektion entdecken';
ru['e.g., Browse our collection'] = 'напр., Посмотрите нашу коллекцию';
tr['e.g., Browse our collection'] = 'örn. Koleksiyonumuza göz atın';
de['e.g., How to care for your lash extensions'] =
  'z. B. So pflegen Sie Ihre Wimpernverlängerungen';
ru['e.g., How to care for your lash extensions'] = 'напр., Как ухаживать за наращенными ресницами';
tr['e.g., How to care for your lash extensions'] = 'örn. Kirpik uzatmalarınıza nasıl bakılır';
de['e.g., OUR SERVICES'] = 'z. B. UNSERE DIENSTE';
ru['e.g., OUR SERVICES'] = 'напр., НАШИ УСЛУГИ';
tr['e.g., OUR SERVICES'] = 'örn. HİZMETLERİMİZ';
de['Edit each storefront page with sections, publish the changes, and preview them live.'] =
  'Bearbeiten Sie jede Storefront-Seite mit Sektionen, veröffentlichen Sie die Änderungen und prüfen Sie sie live.';
ru['Edit each storefront page with sections, publish the changes, and preview them live.'] =
  'Редактируйте каждую страницу витрины с разделами, публикуйте изменения и просматривайте их вживую.';
tr['Edit each storefront page with sections, publish the changes, and preview them live.'] =
  'Her vitrin sayfasını bölümlerle düzenleyin, değişiklikleri yayınlayın ve canlı önizleyin.';
de['EDITING ARTICLE'] = 'ARTIKEL WIRD BEARBEITET';
ru['EDITING ARTICLE'] = 'РЕДАКТИРОВАНИЕ СТАТЬИ';
tr['EDITING ARTICLE'] = 'MAKALE DÜZENLENİYOR';
de['EDITING PAGE'] = 'SEITE WIRD BEARBEITET';
ru['EDITING PAGE'] = 'РЕДАКТИРОВАНИЕ СТРАНИЦЫ';
tr['EDITING PAGE'] = 'SAYFA DÜZENLENİYOR';
de['EDITING'] = 'BEARBEITUNG';
ru['EDITING'] = 'РЕДАКТИРОВАНИЕ';
tr['EDITING'] = 'DÜZENLENİYOR';
de['Email address'] = 'E-Mail-Adresse';
ru['Email address'] = 'Адрес электронной почты';
tr['Email address'] = 'E-posta adresi';
de['Email customer'] = 'Kunden per E-Mail kontaktieren';
ru['Email customer'] = 'Написать клиенту';
tr['Email customer'] = 'Müşteriye e-posta gönder';
de['email hidden in this preview'] = 'E-Mail in dieser Vorschau ausgeblendet';
ru['email hidden in this preview'] = 'e-mail скрыт в этом предпросмотре';
tr['email hidden in this preview'] = 'e-posta bu önizlemede gizli';
de['Enable customer booking. The default duration applies when no active services exist.'] =
  'Aktivieren Sie die Kundenbuchung. Die Standarddauer gilt, wenn keine aktiven Dienste vorhanden sind.';
ru['Enable customer booking. The default duration applies when no active services exist.'] =
  'Включите бронирование клиентами. Стандартная длительность применяется, если нет активных услуг.';
tr['Enable customer booking. The default duration applies when no active services exist.'] =
  'Müşteri rezervasyonunu etkinleştirin. Aktif hizmet yoksa varsayılan süre uygulanır.';
de[
  'Enabled languages appear in the storefront language switcher. Keep at least one language enabled.'
] =
  'Aktivierte Sprachen erscheinen im Sprachumschalter der Storefront. Halten Sie mindestens eine Sprache aktiviert.';
ru[
  'Enabled languages appear in the storefront language switcher. Keep at least one language enabled.'
] =
  'Включённые языки появляются в переключателе языка витрины. Оставьте хотя бы один язык включённым.';
tr[
  'Enabled languages appear in the storefront language switcher. Keep at least one language enabled.'
] = 'Etkin diller vitrin dil değiştiricisinde görünür. En az bir dili etkin tutun.';
de['Enabled'] = 'Aktiviert';
ru['Enabled'] = 'Включено';
tr['Enabled'] = 'Etkin';
de['Enter a resource ID and select a calendar.'] =
  'Geben Sie eine Ressourcen-ID ein und wählen Sie einen Kalender.';
ru['Enter a resource ID and select a calendar.'] =
  'Введите идентификатор ресурса и выберите календарь.';
tr['Enter a resource ID and select a calendar.'] = 'Bir kaynak kimliği girin ve bir takvim seçin.';
de['Enter one option on each line.'] = 'Geben Sie eine Option pro Zeile ein.';
ru['Enter one option on each line.'] = 'Вводите по одному варианту в каждой строке.';
tr['Enter one option on each line.'] = 'Her satıra bir seçenek girin.';
de['External calendar'] = 'Externer Kalender';
ru['External calendar'] = 'Внешний календарь';
tr['External calendar'] = 'Harici takvim';
de['FAILED'] = 'FEHLGESCHLAGEN';
ru['FAILED'] = 'СБОЙ';
tr['FAILED'] = 'BAŞARISIZ';
de['Field {index}'] = 'Feld {index}';
ru['Field {index}'] = 'Поле {index}';
tr['Field {index}'] = '{index}. alan';
de['Field key'] = 'Feldschlüssel';
ru['Field key'] = 'Ключ поля';
tr['Field key'] = 'Alan anahtarı';
de['Field type'] = 'Feldtyp';
ru['Field type'] = 'Тип поля';
tr['Field type'] = 'Alan türü';
de['FIELDS'] = 'FELDER';
ru['FIELDS'] = 'ПОЛЯ';
tr['FIELDS'] = 'ALANLAR';
de['FIRST PURCHASABLE OPTION'] = 'ERSTE KAUFBARE OPTION';
ru['FIRST PURCHASABLE OPTION'] = 'ПЕРВЫЙ ПОКУПАЕМЫЙ ВАРИАНТ';
tr['FIRST PURCHASABLE OPTION'] = 'İLK SATIN ALINABİLİR SEÇENEK';
de['Flat shipping cost (EUR)'] = 'Fester Versandkostenbetrag (EUR)';
ru['Flat shipping cost (EUR)'] = 'Плоская стоимость доставки (EUR)';
tr['Flat shipping cost (EUR)'] = 'Sabit kargo ücreti (EUR)';
de['Form builder'] = 'Formular-Builder';
ru['Form builder'] = 'Конструктор форм';
tr['Form builder'] = 'Form oluşturucu';
de['FORM BUILDER'] = 'FORMULAR-BUILDER';
ru['FORM BUILDER'] = 'КОНСТРУКТОР ФОРМ';
tr['FORM BUILDER'] = 'FORM OLUŞTURUCU';
de['Form enabled'] = 'Formular aktiviert';
ru['Form enabled'] = 'Форма включена';
tr['Form enabled'] = 'Form etkin';
de['Form list'] = 'Formularliste';
ru['Form list'] = 'Список форм';
tr['Form list'] = 'Form listesi';
de['Form name'] = 'Name des Formulars';
ru['Form name'] = 'Название формы';
tr['Form name'] = 'Form adı';
de['FORM PREVIEW'] = 'FORMULAR-VORSCHAU';
ru['FORM PREVIEW'] = 'ПРЕДПРОСМОТР ФОРМЫ';
tr['FORM PREVIEW'] = 'FORM ÖNİZLEME';
de['Form slug'] = 'Form-Slug';
ru['Form slug'] = 'Slug формы';
tr['Form slug'] = 'Form kimliği';
de['Form submissions'] = 'Formulareinsendungen';
ru['Form submissions'] = 'Отправки форм';
tr['Form submissions'] = 'Form gönderimleri';
de['Form views'] = 'Formansichten';
ru['Form views'] = 'Представления формы';
tr['Form views'] = 'Form görünümleri';
de['Forms'] = 'Formulare';
ru['Forms'] = 'Формы';
tr['Forms'] = 'Formlar';
de['Free shipping from (EUR, empty = never)'] = 'Kostenloser Versand ab (EUR, leer = nie)';
ru['Free shipping from (EUR, empty = never)'] = 'Бесплатная доставка от (EUR, пусто = никогда)';
tr['Free shipping from (EUR, empty = never)'] = 'Ücretsiz kargo başlangıç tutarı (EUR, boş = asla)';
de['FROM'] = 'VON';
ru['FROM'] = 'С';
tr['FROM'] = 'BAŞLANGIÇ';
de['Fulfill order'] = 'Bestellung ausführen';
ru['Fulfill order'] = 'Выполнить заказ';
tr['Fulfill order'] = 'Siparişi tamamla';
de['Fulfilled'] = 'Erfüllt';
ru['Fulfilled'] = 'Выполнено';
tr['Fulfilled'] = 'Tamamlandı';
de['General booking'] = 'Allgemeine Buchung';
ru['General booking'] = 'Общее бронирование';
tr['General booking'] = 'Genel rezervasyon';
de['Go to date'] = 'Zum Datum springen';
ru['Go to date'] = 'Перейти к дате';
tr['Go to date'] = 'Tarihe git';
de['Google Calendar account'] = 'Google-Kalenderkonto';
ru['Google Calendar account'] = 'Аккаунт Google Календаря';
tr['Google Calendar account'] = 'Google Takvim hesabı';
de['Google calendar'] = 'Google-Kalender';
ru['Google calendar'] = 'Календарь Google';
tr['Google calendar'] = 'Google takvimi';
de['Google Calendar'] = 'Google Kalender';
ru['Google Calendar'] = 'Google Календарь';
tr['Google Calendar'] = 'Google Takvim';
de['Heading & Intro'] = 'Überschrift & Einleitung';
ru['Heading & Intro'] = 'Заголовок и вступление';
tr['Heading & Intro'] = 'Başlık ve giriş';
de['Help text'] = 'Hilfetext';
ru['Help text'] = 'Справочный текст';
tr['Help text'] = 'Yardım metni';
de['Hide'] = 'Ausblenden';
ru['Hide'] = 'Скрыть';
tr['Hide'] = 'Gizle';
de['HOW IT WORKS'] = 'SO FUNKTIONIERT ES';
ru['HOW IT WORKS'] = 'КАК ЭТО РАБОТАЕТ';
tr['HOW IT WORKS'] = 'NASIL ÇALIŞIR';
de['Image URL'] = 'Bild-URL';
ru['Image URL'] = 'URL изображения';
tr['Image URL'] = 'Görsel URL';
de['Image Gallery'] = 'Bildergalerie';
ru['Image Gallery'] = 'Галерея изображений';
tr['Image Gallery'] = 'Görsel galerisi';
de['Gallery'] = 'Galerie';
ru['Gallery'] = 'Галерея';
tr['Gallery'] = 'Galeri';
de['Caption'] = 'Bildunterschrift';
ru['Caption'] = 'Подпись';
tr['Caption'] = 'Açıklama';
de['Remove image'] = 'Bild entfernen';
ru['Remove image'] = 'Удалить изображение';
tr['Remove image'] = 'Görseli kaldır';
de['Product images'] = 'Produktbilder';
ru['Product images'] = 'Изображения товара';
tr['Product images'] = 'Ürün görselleri';
de['The first image is the main image. Upload up to 8 JPG, PNG, or WebP images.'] =
  'Das erste Bild ist das Hauptbild. Laden Sie bis zu 8 JPG-, PNG- oder WebP-Bilder hoch.';
ru['The first image is the main image. Upload up to 8 JPG, PNG, or WebP images.'] =
  'Первое изображение используется как основное. Загрузите до 8 файлов JPG, PNG или WebP.';
tr['The first image is the main image. Upload up to 8 JPG, PNG, or WebP images.'] =
  'İlk görsel ana görseldir. En fazla 8 JPG, PNG veya WebP görseli yükleyin.';
de['Choose images or drop them here'] = 'Bilder auswählen oder hier ablegen';
ru['Choose images or drop them here'] = 'Выберите изображения или перетащите их сюда';
tr['Choose images or drop them here'] = 'Görselleri seçin veya buraya bırakın';
de['Uploading images…'] = 'Bilder werden hochgeladen…';
ru['Uploading images…'] = 'Изображения загружаются…';
tr['Uploading images…'] = 'Görseller yükleniyor…';
de['Image limit reached.'] = 'Das Bilderlimit ist erreicht.';
ru['Image limit reached.'] = 'Достигнут лимит изображений.';
tr['Image limit reached.'] = 'Görsel sınırına ulaşıldı.';
de['The images could not be uploaded.'] = 'Die Bilder konnten nicht hochgeladen werden.';
ru['The images could not be uploaded.'] = 'Не удалось загрузить изображения.';
tr['The images could not be uploaded.'] = 'Görseller yüklenemedi.';
de['Editor mode'] = 'Editormodus';
ru['Editor mode'] = 'Режим редактора';
tr['Editor mode'] = 'Düzenleyici modu';
de['Edit'] = 'Bearbeiten';
ru['Edit'] = 'Редактировать';
tr['Edit'] = 'Düzenle';
de['Preview'] = 'Vorschau';
ru['Preview'] = 'Предпросмотр';
tr['Preview'] = 'Önizleme';
de['Product preview'] = 'Produktvorschau';
ru['Product preview'] = 'Предпросмотр товара';
tr['Product preview'] = 'Ürün önizlemesi';
de['Untitled product'] = 'Produkt ohne Namen';
ru['Untitled product'] = 'Товар без названия';
tr['Untitled product'] = 'Adsız ürün';
de['in stock'] = 'auf Lager';
ru['in stock'] = 'в наличии';
tr['in stock'] = 'stokta';
de['The selected file'] = 'Die ausgewählte Datei';
ru['The selected file'] = 'Выбранный файл';
tr['The selected file'] = 'Seçilen dosya';
de['The image "{name}" is larger than 8 MB. Select a smaller image.'] =
  'Das Bild „{name}“ ist größer als 8 MB. Wählen Sie ein kleineres Bild aus.';
ru['The image "{name}" is larger than 8 MB. Select a smaller image.'] =
  'Размер изображения «{name}» превышает 8 МБ. Выберите изображение меньшего размера.';
tr['The image "{name}" is larger than 8 MB. Select a smaller image.'] =
  '“{name}” görseli 8 MB’den büyük. Daha küçük bir görsel seçin.';
de['The file "{name}" is not a supported image. Select a JPG, PNG, or WebP file.'] =
  'Die Datei „{name}“ ist kein unterstütztes Bild. Wählen Sie eine JPG-, PNG- oder WebP-Datei aus.';
ru['The file "{name}" is not a supported image. Select a JPG, PNG, or WebP file.'] =
  'Файл «{name}» имеет неподдерживаемый формат. Выберите файл JPG, PNG или WebP.';
tr['The file "{name}" is not a supported image. Select a JPG, PNG, or WebP file.'] =
  '“{name}” dosyası desteklenen bir görsel değil. JPG, PNG veya WebP dosyası seçin.';
de['You can add up to 8 images. Remove an image before you add another one.'] =
  'Sie können bis zu 8 Bilder hinzufügen. Entfernen Sie ein Bild, bevor Sie ein weiteres hinzufügen.';
ru['You can add up to 8 images. Remove an image before you add another one.'] =
  'Можно добавить до 8 изображений. Удалите одно изображение перед добавлением нового.';
tr['You can add up to 8 images. Remove an image before you add another one.'] =
  'En fazla 8 görsel ekleyebilirsiniz. Yeni bir görsel eklemeden önce bir görseli kaldırın.';
de['Your session has expired. Sign in again, then upload the image.'] =
  'Ihre Sitzung ist abgelaufen. Melden Sie sich erneut an und laden Sie das Bild dann hoch.';
ru['Your session has expired. Sign in again, then upload the image.'] =
  'Срок действия сеанса истёк. Войдите снова, затем загрузите изображение.';
tr['Your session has expired. Sign in again, then upload the image.'] =
  'Oturumunuzun süresi doldu. Tekrar oturum açın ve görseli yükleyin.';
de['The server rejected the upload. Reload the page, sign in again, and retry.'] =
  'Der Server hat den Upload abgelehnt. Laden Sie die Seite neu, melden Sie sich erneut an und versuchen Sie es noch einmal.';
ru['The server rejected the upload. Reload the page, sign in again, and retry.'] =
  'Сервер отклонил загрузку. Перезагрузите страницу, войдите снова и повторите попытку.';
tr['The server rejected the upload. Reload the page, sign in again, and retry.'] =
  'Sunucu yüklemeyi reddetti. Sayfayı yenileyin, tekrar oturum açın ve yeniden deneyin.';
de['The upload server did not respond. Check your connection and retry.'] =
  'Der Upload-Server antwortet nicht. Prüfen Sie Ihre Verbindung und versuchen Sie es erneut.';
ru['The upload server did not respond. Check your connection and retry.'] =
  'Сервер загрузки не отвечает. Проверьте подключение и повторите попытку.';
tr['The upload server did not respond. Check your connection and retry.'] =
  'Yükleme sunucusu yanıt vermedi. Bağlantınızı kontrol edin ve yeniden deneyin.';
de['The server could not store the image. Retry later.'] =
  'Der Server konnte das Bild nicht speichern. Versuchen Sie es später erneut.';
ru['The server could not store the image. Retry later.'] =
  'Сервер не смог сохранить изображение. Повторите попытку позже.';
tr['The server could not store the image. Retry later.'] =
  'Sunucu görseli kaydedemedi. Daha sonra yeniden deneyin.';
de['The image "{name}" could not be uploaded. Retry the upload.'] =
  'Das Bild „{name}“ konnte nicht hochgeladen werden. Versuchen Sie den Upload erneut.';
ru['The image "{name}" could not be uploaded. Retry the upload.'] =
  'Не удалось загрузить изображение «{name}». Повторите загрузку.';
tr['The image "{name}" could not be uploaded. Retry the upload.'] =
  '“{name}” görseli yüklenemedi. Yüklemeyi yeniden deneyin.';
de['Select a JPG, PNG, or WebP image up to 8 MB.'] =
  'Wählen Sie ein JPG-, PNG- oder WebP-Bild mit maximal 8 MB aus.';
ru['Select a JPG, PNG, or WebP image up to 8 MB.'] =
  'Выберите изображение JPG, PNG или WebP размером до 8 МБ.';
tr['Select a JPG, PNG, or WebP image up to 8 MB.'] =
  'En fazla 8 MB boyutunda bir JPG, PNG veya WebP görseli seçin.';
de['Add image'] = 'Bild hinzufügen';
ru['Add image'] = 'Добавить изображение';
tr['Add image'] = 'Görsel ekle';
de['Add the images for this gallery. Each image needs an address; the caption is optional.'] =
  'Fügen Sie die Bilder dieser Galerie hinzu. Jedes Bild benötigt eine Adresse; die Bildunterschrift ist optional.';
ru['Add the images for this gallery. Each image needs an address; the caption is optional.'] =
  'Добавьте изображения этой галереи. Каждому изображению нужен адрес; подпись необязательна.';
tr['Add the images for this gallery. Each image needs an address; the caption is optional.'] =
  'Bu galeriye görseller ekleyin. Her görsel bir adres gerektirir; açıklama isteğe bağlıdır.';
de['Image with Caption'] = 'Bild mit Bildunterschrift';
ru['Image with Caption'] = 'Изображение с подписью';
tr['Image with Caption'] = 'Açıklamalı görsel';
de['Image'] = 'Bild';
ru['Image'] = 'Изображение';
tr['Image'] = 'Görsel';
de['Implementation status'] = 'Umsetzungsstatus';
ru['Implementation status'] = 'Статус реализации';
tr['Implementation status'] = 'Uygulama durumu';
de['Import busy times and send confirmed bookings to a mapped calendar.'] =
  'Importieren Sie Besetzungszeiten und senden Sie bestätigte Buchungen an einen zugeordneten Kalender.';
ru['Import busy times and send confirmed bookings to a mapped calendar.'] =
  'Импортируйте занятые интервалы и отправляйте подтверждённые бронирования в сопоставленный календарь.';
tr['Import busy times and send confirmed bookings to a mapped calendar.'] =
  'Meşgul saatleri içe aktarın ve onaylı rezervasyonları eşlenmiş takvime gönderin.';
de['Input fields and validation'] = 'Eingabefelder und Validierung';
ru['Input fields and validation'] = 'Поля ввода и проверка';
tr['Input fields and validation'] = 'Giriş alanları ve doğrulama';
de['INTEGRATIONS'] = 'INTEGRATIONEN';
ru['INTEGRATIONS'] = 'ИНТЕГРАЦИИ';
tr['INTEGRATIONS'] = 'ENTEGRELER';
de['Interface language'] = 'Oberflächensprache';
ru['Interface language'] = 'Язык интерфейса';
tr['Interface language'] = 'Arayüz dili';
de['INTERNATIONALIZATION'] = 'INTERNATIONALISIERUNG';
ru['INTERNATIONALIZATION'] = 'ИНТЕРНАЦИОНАЛИЗАЦИЯ';
tr['INTERNATIONALIZATION'] = 'ULUSLARARASILAŞTIRMA';
de['INVENTORY ALERTS'] = 'BESTANDSWARNUNGEN';
ru['INVENTORY ALERTS'] = 'ОПОВЕЩЕНИЯ О ЗАПАСАХ';
tr['INVENTORY ALERTS'] = 'STOK UYARILARI';
de['OpenShopfront ADMINISTRATION'] = 'OpenShopfront-VERWALTUNG';
ru['OpenShopfront ADMINISTRATION'] = 'УПРАВЛЕНИЕ OpenShopfront';
tr['OpenShopfront ADMINISTRATION'] = 'OpenShopfront YÖNETİMİ';
de['Italic'] = 'Kursiv';
ru['Italic'] = 'Курсив';
tr['Italic'] = 'İtalik';
de['Items'] = 'Artikel';
ru['Items'] = 'Позиции';
tr['Items'] = 'Ürünler';
de['Keep at least one language enabled.'] = 'Halten Sie mindestens eine Sprache aktiviert.';
ru['Keep at least one language enabled.'] = 'Оставьте хотя бы один язык включённым.';
tr['Keep at least one language enabled.'] = 'En az bir dili etkin tutun.';
de['Label'] = 'Beschriftung';
ru['Label'] = 'Метка';
tr['Label'] = 'Etiket';
de['Last 30 days'] = 'Letzte 30 Tage';
ru['Last 30 days'] = 'Последние 30 дней';
tr['Last 30 days'] = 'Son 30 gün';
de['Last 7 days'] = 'Letzte 7 Tage';
ru['Last 7 days'] = 'Последние 7 дней';
tr['Last 7 days'] = 'Son 7 gün';
de['Last 90 days'] = 'Letzte 90 Tage';
ru['Last 90 days'] = 'Последние 90 дней';
tr['Last 90 days'] = 'Son 90 gün';
de['Learn more'] = 'Mehr erfahren';
ru['Learn more'] = 'Узнать больше';
tr['Learn more'] = 'Daha fazla öğren';
de['Link to page'] = 'Verlinkung zur Seite';
ru['Link to page'] = 'Ссылка на страницу';
tr['Link to page'] = 'Sayfaya bağlantı';
de['LIVE ON STOREFRONT'] = 'AUF DER STOREFRONT AKTIV';
ru['LIVE ON STOREFRONT'] = 'ОПУБЛИКОВАНО НА ВИТРИНЕ';
tr['LIVE ON STOREFRONT'] = 'VİTRİNDE YAYINDA';
de['LIVE SCHEDULE'] = 'LIVE-PLANUNG';
ru['LIVE SCHEDULE'] = 'РАСПИСАНИЕ В ЖИВОЙ РЕЖИМЕ';
tr['LIVE SCHEDULE'] = 'CANLI TAKVİM';
de['Load content.'] = 'Inhalt wird geladen.';
ru['Load content.'] = 'Загрузка контента.';
tr['Load content.'] = 'İçerik yükleniyor.';
de['Load the product to edit its options.'] =
  'Laden Sie das Produkt, um seine Optionen zu bearbeiten.';
ru['Load the product to edit its options.'] = 'Загрузите товар, чтобы редактировать его варианты.';
tr['Load the product to edit its options.'] = 'Seçeneklerini düzenlemek için ürünü yükleyin.';
de['Loading the commerce workspace…'] = 'Commerce-Arbeitsbereich wird geladen…';
ru['Loading the commerce workspace…'] = 'Загрузка рабочей области торговли…';
tr['Loading the commerce workspace…'] = 'Ticaret çalışma alanı yükleniyor…';
de['Loading...'] = 'Wird geladen…';
ru['Loading...'] = 'Загрузка…';
tr['Loading...'] = 'Yükleniyor…';
de['Loading'] = 'Lädt';
ru['Loading'] = 'Загрузка';
tr['Loading'] = 'Yükleniyor';
de['Locale'] = 'Spracheinstellung';
ru['Locale'] = 'Локаль';
tr['Locale'] = 'Bölgesel ayar';
de['Long text'] = 'Langer Text';
ru['Long text'] = 'Длинный текст';
tr['Long text'] = 'Uzun metin';
de['Lowest available stock first'] = 'Zuerst der niedrigste verfügbare Bestand';
ru['Lowest available stock first'] = 'Сначала наименьший доступный запас';
tr['Lowest available stock first'] = 'Önce en düşük mevcut stok';
de['Main heading'] = 'Hauptüberschrift';
ru['Main heading'] = 'Основной заголовок';
tr['Main heading'] = 'Ana başlık';
de['Manage Articles'] = 'Artikel verwalten';
ru['Manage Articles'] = 'Управлять статьями';
tr['Manage Articles'] = 'Makaleleri yönet';
de['Manage products, stock, and payments'] = 'Produkte, Bestand und Zahlungen verwalten';
ru['Manage products, stock, and payments'] = 'Управление товарами, запасами и платежами';
tr['Manage products, stock, and payments'] = 'Ürünleri, stokları ve ödemeleri yönetin';
de['Manage products, stock, orders, fulfillment, refunds, and shop settings.'] =
  'Verwalten Sie Produkte, Bestand, Bestellungen, Auslieferung, Erstattungen und Shop-Einstellungen.';
ru['Manage products, stock, orders, fulfillment, refunds, and shop settings.'] =
  'Управляйте товарами, запасами, заказами, выполнением, возвратами и настройками магазина.';
tr['Manage products, stock, orders, fulfillment, refunds, and shop settings.'] =
  'Ürünleri, stokları, siparişleri, sevkiyatı, iadeleri ve mağaza ayarlarını yönetin.';
de['Mark paid'] = 'Als bezahlt markieren';
ru['Mark paid'] = 'Отметить оплаченным';
tr['Mark paid'] = 'Ödendi olarak işaretle';
de['Maximum length'] = 'Maximale Länge';
ru['Maximum length'] = 'Максимальная длина';
tr['Maximum length'] = 'Maksimum uzunluk';
de['min'] = 'Min.';
ru['min'] = 'мин';
tr['min'] = 'dk';
de['Minimum length'] = 'Minimale Länge';
ru['Minimum length'] = 'Минимальная длина';
tr['Minimum length'] = 'Minimum uzunluk';
de['minutes'] = 'Minuten';
ru['minutes'] = 'минуты';
tr['minutes'] = 'dakika';
de['Move down'] = 'Nach unten verschieben';
ru['Move down'] = 'Переместить вниз';
tr['Move down'] = 'Aşağı taşı';
de['Move field down'] = 'Feld nach unten verschieben';
ru['Move field down'] = 'Переместить поле вниз';
tr['Move field down'] = 'Alanı aşağı taşı';
de['Move field up'] = 'Feld nach oben verschieben';
ru['Move field up'] = 'Переместить поле вверх';
tr['Move field up'] = 'Alanı yukarı taşı';
de['Move up'] = 'Nach oben verschieben';
ru['Move up'] = 'Переместить вверх';
tr['Move up'] = 'Yukarı taşı';
de['Name'] = 'Name';
ru['Name'] = 'Название';
tr['Name'] = 'Ad';
de['Navigate pages'] = 'Seiten navigieren';
ru['Navigate pages'] = 'Навигация по страницам';
tr['Navigate pages'] = 'Sayfalarda gezin';
de['New Article'] = 'Neuer Artikel';
ru['New Article'] = 'Новая статья';
tr['New Article'] = 'Yeni makale';
de['New form'] = 'Neues Formular';
ru['New form'] = 'Новая форма';
tr['New form'] = 'Yeni form';
de['New label'] = 'Neue Beschriftung';
ru['New label'] = 'Новая метка';
tr['New label'] = 'Yeni etiket';
de['NEW PRODUCT'] = 'NEUES PRODUKT';
ru['NEW PRODUCT'] = 'НОВЫЙ ТОВАР';
tr['NEW PRODUCT'] = 'YENİ ÜRÜN';
de['NEW SERVICE'] = 'NEUER DIENST';
ru['NEW SERVICE'] = 'НОВАЯ УСЛУГА';
tr['NEW SERVICE'] = 'YENİ HİZMET';
de['New SKU'] = 'Neue SKU';
ru['New SKU'] = 'Новый артикул';
tr['New SKU'] = 'Yeni stok kodu';
de['Next week'] = 'Nächste Woche';
ru['Next week'] = 'Следующая неделя';
tr['Next week'] = 'Sonraki hafta';
de['Next'] = 'Weiter';
ru['Next'] = 'Далее';
tr['Next'] = 'İleri';
de['No active products.'] = 'Keine aktiven Produkte.';
ru['No active products.'] = 'Нет активных товаров.';
tr['No active products.'] = 'Aktif ürün yok.';
de['No bookings'] = 'Keine Buchungen';
ru['No bookings'] = 'Нет бронирований';
tr['No bookings'] = 'Rezervasyon yok';
de['No business figures are available yet.'] = 'Es sind noch keine Geschäftszahlen verfügbar.';
ru['No business figures are available yet.'] = 'Показатели бизнеса пока недоступны.';
tr['No business figures are available yet.'] = 'Henüz iş rakamı mevcut değil.';
de['No calendar mapping'] = 'Keine Kalenderzuordnung';
ru['No calendar mapping'] = 'Нет сопоставления календаря';
tr['No calendar mapping'] = 'Takvim eşlemesi yok';
de['No email address'] = 'Keine E-Mail-Adresse';
ru['No email address'] = 'Нет адреса электронной почты';
tr['No email address'] = 'E-posta adresi yok';
de['No forms exist.'] = 'Es existieren keine Formulare.';
ru['No forms exist.'] = 'Формы отсутствуют.';
tr['No forms exist.'] = 'Form bulunmuyor.';
de['No Google services account is connected.'] = 'Kein Google-Dienste-Konto ist verbunden.';
ru['No Google services account is connected.'] = 'Аккаунт сервисов Google не подключён.';
tr['No Google services account is connected.'] = 'Bağlı bir Google hizmetler hesabı yok.';
de['no key'] = 'kein Schlüssel';
ru['no key'] = 'нет ключа';
tr['no key'] = 'anahtar yok';
de['No orders in this range.'] = 'Keine Bestellungen in diesem Zeitraum.';
ru['No orders in this range.'] = 'Заказов за этот период нет.';
tr['No orders in this range.'] = 'Bu aralıkta sipariş yok.';
de['No orders match this filter yet.'] = 'Keine Bestellungen entsprechen noch diesem Filter.';
ru['No orders match this filter yet.'] = 'Пока ни один заказ не соответствует этому фильтру.';
tr['No orders match this filter yet.'] = 'Bu filtreye uyan sipariş henüz yok.';
de['NO PAYMENT PROVIDER'] = 'KEIN ZAHLUNGSANBIETER';
ru['NO PAYMENT PROVIDER'] = 'НЕТ ПЛАТЁЖНОГО ПРОВАЙДЕРА';
tr['NO PAYMENT PROVIDER'] = 'ÖDEME SAĞLAYICISI YOK';
de['No payment recorded yet.'] = 'Es wurde noch keine Zahlung erfasst.';
ru['No payment recorded yet.'] = 'Платёж пока не зафиксирован.';
tr['No payment recorded yet.'] = 'Henüz ödeme kaydedilmedi.';
de['No products yet. Create your first product on the right.'] =
  'Noch keine Produkte. Erstellen Sie rechts Ihr erstes Produkt.';
ru['No products yet. Create your first product on the right.'] =
  'Товаров пока нет. Создайте первый товар справа.';
tr['No products yet. Create your first product on the right.'] =
  'Henüz ürün yok. İlk ürününüzü sağda oluşturun.';
de['No resource has a calendar mapping.'] = 'Keine Ressource hat eine Kalenderzuordnung.';
ru['No resource has a calendar mapping.'] = 'Ни у одного ресурса нет сопоставления календаря.';
tr['No resource has a calendar mapping.'] = 'Takvim eşlemesi olan kaynak yok.';
de['No services exist'] = 'Keine Dienste vorhanden';
ru['No services exist'] = 'Услуги отсутствуют';
tr['No services exist'] = 'Hizmet bulunmuyor';
de['No status data is available.'] = 'Es sind keine Statusdaten verfügbar.';
ru['No status data is available.'] = 'Данные статуса недоступны.';
tr['No status data is available.'] = 'Durum verisi mevcut değil.';
de['No submissions yet.'] = 'Noch keine Einsendungen.';
ru['No submissions yet.'] = 'Отправок пока нет.';
tr['No submissions yet.'] = 'Henüz gönderim yok.';
de['no tracking number'] = 'keine Sendungsnummer';
ru['no tracking number'] = 'нет номера отслеживания';
tr['no tracking number'] = 'takip numarası yok';
de['No units sold in this range.'] = 'Keine Einheiten in diesem Zeitraum verkauft.';
ru['No units sold in this range.'] = 'За этот период ничего не продано.';
tr['No units sold in this range.'] = 'Bu aralıkta satılan adet yok.';
de['No'] = 'Nein';
ru['No'] = 'Нет';
tr['No'] = 'Hayır';
de['not available'] = 'nicht verfügbar';
ru['not available'] = 'недоступно';
tr['not available'] = 'mevcut değil';
de['NOT CONNECTED'] = 'NICHT VERBUNDEN';
ru['NOT CONNECTED'] = 'НЕ ПОДКЛЮЧЕНО';
tr['NOT CONNECTED'] = 'BAĞLI DEĞİL';
de['NOT IMPLEMENTED'] = 'NICHT UMGESETZT';
ru['NOT IMPLEMENTED'] = 'НЕ РЕАЛИЗОВАНО';
tr['NOT IMPLEMENTED'] = 'UYGULANMADI';
de['Not recorded'] = 'Nicht erfasst';
ru['Not recorded'] = 'Не указано';
tr['Not recorded'] = 'Kaydedilmemiş';
de['Number of products to display'] = 'Anzahl der anzuzeigenden Produkte';
ru['Number of products to display'] = 'Количество отображаемых товаров';
tr['Number of products to display'] = 'Gösterilecek ürün sayısı';
de['OFF'] = 'AUS';
ru['OFF'] = 'ВЫКЛ.';
tr['OFF'] = 'KAPALI';
de['ON'] = 'AN';
ru['ON'] = 'ВКЛ.';
tr['ON'] = 'AÇIK';
de['Online appointments'] = 'Online-Termine';
ru['Online appointments'] = 'Онлайн-визиты';
tr['Online appointments'] = 'Çevrimiçi randevular';
de['Open bookings'] = 'Buchungen öffnen';
ru['Open bookings'] = 'Открыть бронирования';
tr['Open bookings'] = 'Rezervasyonları aç';
de['Open submission details'] = 'Details der Einsendung öffnen';
ru['Open submission details'] = 'Открыть детали отправки';
tr['Open submission details'] = 'Gönderim detaylarını aç';
de['Open the current Strapi workspace'] = 'Aktuellen Strapi-Arbeitsbereich öffnen';
ru['Open the current Strapi workspace'] = 'Открыть текущую рабочую область Strapi';
tr['Open the current Strapi workspace'] = 'Mevcut Strapi çalışma alanını açın';
de['Open the shop to customers'] = 'Shop für Kunden öffnen';
ru['Open the shop to customers'] = 'Открыть магазин для клиентов';
tr['Open the shop to customers'] = 'Mağazayı müşterilere aç';
de['Open workspace'] = 'Arbeitsbereich öffnen';
ru['Open workspace'] = 'Открыть рабочую область';
tr['Open workspace'] = 'Çalışma alanını aç';
de['OPERATIONS ANALYTICS'] = 'BETRIEBSANALYSE';
ru['OPERATIONS ANALYTICS'] = 'АНАЛИТИКА ОПЕРАЦИЙ';
tr['OPERATIONS ANALYTICS'] = 'İŞLEM ANALİTİĞİ';
de['Operations'] = 'Betrieb';
ru['Operations'] = 'Операции';
tr['Operations'] = 'Operasyonlar';
de['OPERATIONS'] = 'BETRIEB';
ru['OPERATIONS'] = 'ОПЕРАЦИИ';
tr['OPERATIONS'] = 'OPERASYONLAR';
de['Order ledger CSV'] = 'Bestellbuch-CSV';
ru['Order ledger CSV'] = 'CSV журнала заказов';
tr['Order ledger CSV'] = 'Sipariş defteri CSV';
de['Order lifecycle'] = 'Lebenszyklus der Bestellung';
ru['Order lifecycle'] = 'Жизненный цикл заказа';
tr['Order lifecycle'] = 'Sipariş yaşam döngüsü';
de['Order status filter'] = 'Filter nach Bestellstatus';
ru['Order status filter'] = 'Фильтр по статусу заказа';
tr['Order status filter'] = 'Sipariş durumu filtresi';
de['ORDER'] = 'BESTELLUNG';
ru['ORDER'] = 'ЗАКАЗ';
tr['ORDER'] = 'SİPARİŞ';
de['Orders and revenue per day (UTC)'] = 'Bestellungen und Umsatz pro Tag (UTC)';
ru['Orders and revenue per day (UTC)'] = 'Заказы и выручка по дням (UTC)';
tr['Orders and revenue per day (UTC)'] = 'Günlük sipariş ve gelir (UTC)';
de['Orders are created as PENDING_PAYMENT. Mark them paid manually.'] =
  'Bestellungen werden als PENDING_PAYMENT erstellt. Markieren Sie sie manuell als bezahlt.';
ru['Orders are created as PENDING_PAYMENT. Mark them paid manually.'] =
  'Заказы создаются со статусом PENDING_PAYMENT. Отмечайте их как оплаченные вручную.';
tr['Orders are created as PENDING_PAYMENT. Mark them paid manually.'] =
  'Siparişler PENDING_PAYMENT olarak oluşturulur. Manuel olarak ödendi işaretleyin.';
de['Orders by status'] = 'Bestellungen nach Status';
ru['Orders by status'] = 'Заказы по статусам';
tr['Orders by status'] = 'Duruma göre siparişler';
de['Orders'] = 'Bestellungen';
ru['Orders'] = 'Заказы';
tr['Orders'] = 'Siparişler';
de['Owner'] = 'Verantwortlich';
ru['Owner'] = 'Ответственный';
tr['Owner'] = 'Sorumlu';
de['Page {current} of {total}'] = 'Seite {current} von {total}';
ru['Page {current} of {total}'] = 'Страница {current} из {total}';
tr['Page {current} of {total}'] = '{total} sayfanın {current}. sayfası';
de['Page editor'] = 'Seiten-Editor';
ru['Page editor'] = 'Редактор страниц';
tr['Page editor'] = 'Sayfa düzenleyici';
de['Page enabled'] = 'Seite aktiviert';
ru['Page enabled'] = 'Страница включена';
tr['Page enabled'] = 'Sayfa etkin';
de['Page tree'] = 'Seitenbaum';
ru['Page tree'] = 'Дерево страниц';
tr['Page tree'] = 'Sayfa ağacı';
de['Paid orders'] = 'Bezahlte Bestellungen';
ru['Paid orders'] = 'Оплаченные заказы';
tr['Paid orders'] = 'Ödenmiş siparişler';
de['PAID ORDERS'] = 'BEZAHLTE BESTELLUNGEN';
ru['PAID ORDERS'] = 'ОПЛАЧЕННЫЕ ЗАКАЗЫ';
tr['PAID ORDERS'] = 'ÖDENMİŞ SİPARİŞLER';
de['Paid'] = 'Bezahlt';
ru['Paid'] = 'Оплачено';
tr['Paid'] = 'Ödendi';
de['PAID'] = 'BEZAHLT';
ru['PAID'] = 'ОПЛАЧЕНО';
tr['PAID'] = 'ÖDENMİŞ';
de['Paragraph / Text Block'] = 'Absatz / Textblock';
ru['Paragraph / Text Block'] = 'Абзац / текстовый блок';
tr['Paragraph / Text Block'] = 'Paragraf / metin bloğu';
de['Password'] = 'Passwort';
ru['Password'] = 'Пароль';
tr['Password'] = 'Şifre';
de['payment confirmed (Stripe webhook or manual), hold released.'] =
  'Zahlung bestätigt (Stripe-Webhook oder manuell), Reservierung freigegeben.';
ru['payment confirmed (Stripe webhook or manual), hold released.'] =
  'платёж подтверждён (webhook Stripe или вручную), резервирование снято.';
tr['payment confirmed (Stripe webhook or manual), hold released.'] =
  'ödeme onaylandı (Stripe webhook veya manuel), bloke kaldırıldı.';
de['Payment'] = 'Zahlung';
ru['Payment'] = 'Платёж';
tr['Payment'] = 'Ödeme';
de['Pending payment'] = 'Ausstehende Zahlung';
ru['Pending payment'] = 'Ожидают оплаты';
tr['Pending payment'] = 'Ödeme bekleniyor';
de['PENDING'] = 'AUSSTEHEND';
ru['PENDING'] = 'ОЖИДАЕТСЯ';
tr['PENDING'] = 'BEKLEMEDE';
de['PERSONAL SETTINGS'] = 'PERSÖNLICHE EINSTELLUNGEN';
ru['PERSONAL SETTINGS'] = 'ЛИЧНЫЕ НАСТРОЙКИ';
tr['PERSONAL SETTINGS'] = 'KİŞİSEL AYARLAR';
de['Phone'] = 'Telefon';
ru['Phone'] = 'Телефон';
tr['Phone'] = 'Telefon';
de['Placeholder'] = 'Platzhalter';
ru['Placeholder'] = 'Плейсхолдер';
tr['Placeholder'] = 'Yer tutucu';
de['Previous view'] = 'Vorherige Ansicht';
ru['Previous view'] = 'Предыдущий вид';
tr['Previous view'] = 'Önceki görünüm';
de['Previous week'] = 'Vorherige Woche';
ru['Previous week'] = 'Прошлая неделя';
tr['Previous week'] = 'Önceki hafta';
de['Previous'] = 'Zurück';
ru['Previous'] = 'Назад';
tr['Previous'] = 'Geri';
de['Price (EUR)'] = 'Preis (EUR)';
ru['Price (EUR)'] = 'Цена (EUR)';
tr['Price (EUR)'] = 'Fiyat (EUR)';
de['Price'] = 'Preis';
ru['Price'] = 'Цена';
tr['Price'] = 'Fiyat';
de['primary'] = 'primär';
ru['primary'] = 'основной';
tr['primary'] = 'birincil';
de['Product / variant'] = 'Produkt / Variante';
ru['Product / variant'] = 'Товар / вариант';
tr['Product / variant'] = 'Ürün / varyant';
de['Product Grid (from Shop)'] = 'Produktgitter (aus dem Shop)';
ru['Product Grid (from Shop)'] = 'Сетка товаров (из магазина)';
tr['Product Grid (from Shop)'] = 'Ürün ızgarası (mağazadan)';
de['Product Grid'] = 'Produktgitter';
ru['Product Grid'] = 'Сетка товаров';
tr['Product Grid'] = 'Ürün ızgarası';
de['Product'] = 'Produkt';
ru['Product'] = 'Товар';
tr['Product'] = 'Ürün';
de['Products'] = 'Produkte';
ru['Products'] = 'Товары';
tr['Products'] = 'Ürünler';
de['Publish form'] = 'Formular veröffentlichen';
ru['Publish form'] = 'Опубликовать форму';
tr['Publish form'] = 'Formu yayınla';
de['Publish page'] = 'Seite veröffentlichen';
ru['Publish page'] = 'Опубликовать страницу';
tr['Publish page'] = 'Sayfayı yayınla';
de['PUBLISHED BUT DISABLED'] = 'VERÖFFENTLICHT, ABER DEAKTIVIERT';
ru['PUBLISHED BUT DISABLED'] = 'ОПУБЛИКОВАНО, НО ОТКЛЮЧЕНО';
tr['PUBLISHED BUT DISABLED'] = 'YAYINLANDI AMA DEVRE DIŞI';
de['PUBLISHED'] = 'VERÖFFENTLICHT';
ru['PUBLISHED'] = 'ОПУБЛИКОВАНО';
tr['PUBLISHED'] = 'YAYINDA';
de['QUICK ACCESS'] = 'SCHNELLZUGRIFF';
ru['QUICK ACCESS'] = 'БЫСТРЫЙ ДОСТУП';
tr['QUICK ACCESS'] = 'HIZLI ERİŞİM';
de['RECENT ORDERS'] = 'LETZTE BESTELLUNGEN';
ru['RECENT ORDERS'] = 'ПОСЛЕДНИЕ ЗАКАЗЫ';
tr['RECENT ORDERS'] = 'SON SİPARİŞLER';
de['Refresh preview'] = 'Vorschau aktualisieren';
ru['Refresh preview'] = 'Обновить предпросмотр';
tr['Refresh preview'] = 'Önizlemeyi yenile';
de['Refresh'] = 'Aktualisieren';
ru['Refresh'] = 'Обновить';
tr['Refresh'] = 'Yenile';
de['Refund order'] = 'Bestellung erstatten';
ru['Refund order'] = 'Возврат заказа';
tr['Refund order'] = 'Siparişi iade et';
de['REFUNDED'] = 'ERSTATTET';
ru['REFUNDED'] = 'ВОЗВРАТ';
tr['REFUNDED'] = 'İADE EDİLDİ';
de['Remove field'] = 'Feld entfernen';
ru['Remove field'] = 'Удалить поле';
tr['Remove field'] = 'Alanı kaldır';
de['Remove option'] = 'Option entfernen';
ru['Remove option'] = 'Удалить вариант';
tr['Remove option'] = 'Seçeneği kaldır';
de['Remove section'] = 'Sektion entfernen';
ru['Remove section'] = 'Удалить раздел';
tr['Remove section'] = 'Bölümü kaldır';
de['Remove'] = 'Entfernen';
ru['Remove'] = 'Удалить';
tr['Remove'] = 'Kaldır';
de['Reports and monitoring'] = 'Berichte und Überwachung';
ru['Reports and monitoring'] = 'Отчёты и мониторинг';
tr['Reports and monitoring'] = 'Raporlar ve izleme';
de['Reports refreshed.'] = 'Berichte aktualisiert.';
ru['Reports refreshed.'] = 'Отчёты обновлены.';
tr['Reports refreshed.'] = 'Raporlar yenilendi.';
de['Reports'] = 'Berichte';
ru['Reports'] = 'Отчёты';
tr['Reports'] = 'Raporlar';
de['Required field'] = 'Pflichtfeld';
ru['Required field'] = 'Обязательное поле';
tr['Required field'] = 'Zorunlu alan';
de['Resource ID'] = 'Ressourcen-ID';
ru['Resource ID'] = 'Идентификатор ресурса';
tr['Resource ID'] = 'Kaynak kimliği';
de['RESOURCE MAPPING'] = 'RESSOURCENZUORDNUNG';
ru['RESOURCE MAPPING'] = 'СОПОСТАВЛЕНИЕ РЕСУРСОВ';
tr['RESOURCE MAPPING'] = 'KAYNAK EŞLEME';
de['Resource'] = 'Ressource';
ru['Resource'] = 'Ресурс';
tr['Resource'] = 'Kaynak';
de['Reusable form'] = 'Wiederverwendbares Formular';
ru['Reusable form'] = 'Переиспользуемая форма';
tr['Reusable form'] = 'Yeniden kullanılabilir form';
de['Reusable Form'] = 'Wiederverwendbares Formular';
ru['Reusable Form'] = 'Переиспользуемая форма';
tr['Reusable Form'] = 'Yeniden kullanılabilir form';
de['REUSABLE FORMS'] = 'WIEDERVERWENDBARE FORMULARE';
ru['REUSABLE FORMS'] = 'ПЕРЕИСПОЛЬЗУЕМЫЕ ФОРМЫ';
tr['REUSABLE FORMS'] = 'YENİDEN KULLANILABİLİR FORMLAR';
de['REVENUE (GROSS)'] = 'UMSATZ (BRUTTO)';
ru['REVENUE (GROSS)'] = 'ВЫРУЧКА (ВАЛОВАЯ)';
tr['REVENUE (GROSS)'] = 'GELİR (HAM)';
de['Revenue'] = 'Umsatz';
ru['Revenue'] = 'Выручка';
tr['Revenue'] = 'Gelir';
de['Review and manage customer bookings'] = 'Kundenbuchungen prüfen und verwalten';
ru['Review and manage customer bookings'] = 'Проверка и управление бронированиями клиентов';
tr['Review and manage customer bookings'] = 'Müşteri rezervasyonlarını inceleyin ve yönetin';
de['Review customer bookings, Calendar delivery, and weekly capacity.'] =
  'Prüfen Sie Kundenbuchungen, Kalenderzustellung und wöchentliche Kapazität.';
ru['Review customer bookings, Calendar delivery, and weekly capacity.'] =
  'Проверяйте бронирования клиентов, доставку в календарь и недельную вместимость.';
tr['Review customer bookings, Calendar delivery, and weekly capacity.'] =
  'Müşteri rezervasyonlarını, takvim teslimatını ve haftalık kapasiteyi inceleyin.';
de['Review implemented capabilities'] = 'Umgesetzte Funktionen prüfen';
ru['Review implemented capabilities'] = 'Проверить реализованные функции';
tr['Review implemented capabilities'] = 'Uygulanan özellikleri inceleyin';
de['Review the messages customers send'] = 'Nachrichten der Kunden prüfen';
ru['Review the messages customers send'] = 'Просмотреть сообщения клиентов';
tr['Review the messages customers send'] = 'Müşterilerin gönderdiği mesajları inceleyin';
de['Rich text editor'] = 'Rich-Text-Editor';
ru['Rich text editor'] = 'Редактор форматированного текста';
tr['Rich text editor'] = 'Zengin metin düzenleyici';
de['Mock test data'] = 'Mock-Testdaten';
ru['Mock test data'] = 'Мок-данные для тестов';
tr['Mock test data'] = 'Sahte test verileri';
de[
  'Fill the application with realistic content for one of these stores. Existing products, services and page content are replaced.'
] =
  'Füllt die Anwendung mit realistischen Inhalten für einen dieser Läden. Vorhandene Produkte, Dienste und Seiteninhalte werden ersetzt.';
ru[
  'Fill the application with realistic content for one of these stores. Existing products, services and page content are replaced.'
] =
  'Заполняет приложение реалистичными данными одного из магазинов. Существующие товары, услуги и страницы будут заменены.';
tr[
  'Fill the application with realistic content for one of these stores. Existing products, services and page content are replaced.'
] =
  'Uygulamayı bu mağazalardan biri için gerçekçi içerikle doldurur. Mevcut ürünler, hizmetler ve sayfa içerikleri değiştirilir.';
de['Shoe Store'] = 'Schuhladen';
ru['Shoe Store'] = 'Обувной магазин';
tr['Shoe Store'] = 'Ayakkabı mağazası';
de['Flower Store'] = 'Blumenladen';
ru['Flower Store'] = 'Цветочный магазин';
tr['Flower Store'] = 'Çiçekçi';
de['Tattoo Studio'] = 'Tätowierstudio';
ru['Tattoo Studio'] = 'Тату-студия';
tr['Tattoo Studio'] = 'Dövme stüdyosu';
de['Bakery'] = 'Bäckerei';
ru['Bakery'] = 'Пекарня';
tr['Bakery'] = 'Fırın';
de['Fashion Store'] = 'Modegeschäft';
ru['Fashion Store'] = 'Магазин одежды';
tr['Fashion Store'] = 'Moda mağazası';
de['Applying…'] = 'Wird angewendet…';
ru['Applying…'] = 'Применяется…';
tr['Applying…'] = 'Uygulanıyor…';
de['Test data applied: {products} products, {services} services and {pages} pages.'] =
  'Testdaten angewendet: {products} Produkte, {services} Dienste und {pages} Seiten.';
ru['Test data applied: {products} products, {services} services and {pages} pages.'] =
  'Тестовые данные применены: {products} товаров, {services} услуг и {pages} страниц.';
tr['Test data applied: {products} products, {services} services and {pages} pages.'] =
  'Test verileri uygulandı: {products} ürün, {services} hizmet ve {pages} sayfa.';
de['The test data could not be applied. Try again.'] =
  'Die Testdaten konnten nicht angewendet werden. Versuchen Sie es erneut.';
ru['The test data could not be applied. Try again.'] =
  'Не удалось применить тестовые данные. Попробуйте ещё раз.';
tr['The test data could not be applied. Try again.'] =
  'Test verileri uygulanamadı. Tekrar deneyin.';
de['Replace all content?'] = 'Alle Inhalte ersetzen?';
ru['Replace all content?'] = 'Заменить всё содержимое?';
tr['Replace all content?'] = 'Tüm içerik değiştirilsin mi?';
de[
  'This deletes every product, booking service and page in the application. Only the {store} test data will remain.'
] =
  'Dies löscht alle Produkte, Dienste und Seiten der Anwendung. Es bleibt nur die Testdaten von {store} übrig.';
ru[
  'This deletes every product, booking service and page in the application. Only the {store} test data will remain.'
] =
  'Это удалит все товары, услуги и страницы приложения. Останутся только тестовые данные «{store}».';
tr[
  'This deletes every product, booking service and page in the application. Only the {store} test data will remain.'
] =
  'Bu, uygulamadaki tüm ürünleri, hizmetleri ve sayfaları siler. Yalnızca {store} test verileri kalır.';
de['Replace all content'] = 'Alle Inhalte ersetzen';
ru['Replace all content'] = 'Заменить всё содержимое';
tr['Replace all content'] = 'Tüm içeriği değiştir';
de['Run checks'] = 'Prüfungen ausführen';
ru['Run checks'] = 'Выполнить проверки';
tr['Run checks'] = 'Denetimleri çalıştır';
de['SALES FUNNEL'] = 'VERKAUFSTRAUCHER';
ru['SALES FUNNEL'] = 'ВОРОНКА ПРОДАЖ';
tr['SALES FUNNEL'] = 'SATIŞ HUNİSİ';
de['Sales, revenue, and booking figures by range'] =
  'Verkaufs-, Umsatz- und Buchungszahlen nach Zeitraum';
ru['Sales, revenue, and booking figures by range'] = 'Продажи, выручка и бронирования по периодам';
tr['Sales, revenue, and booking figures by range'] =
  'Aralığa göre satış, gelir ve rezervasyon rakamları';
de[
  'Sales, revenue, funnel, inventory and booking figures for a date range. All amounts are EUR; dates follow UTC. Export CSV where required.'
] =
  'Verkaufs-, Umsatz-, Trichter-, Bestands- und Buchungszahlen für einen Zeitraum. Alle Beträge in EUR; Datumsangaben folgen UTC. CSV exportieren, wo erforderlich.';
ru[
  'Sales, revenue, funnel, inventory and booking figures for a date range. All amounts are EUR; dates follow UTC. Export CSV where required.'
] =
  'Продажи, выручка, воронка, запасы и бронирования за период. Все суммы в EUR; даты по UTC. Экспортируйте CSV при необходимости.';
tr[
  'Sales, revenue, funnel, inventory and booking figures for a date range. All amounts are EUR; dates follow UTC. Export CSV where required.'
] =
  'Bir tarih aralığı için satış, gelir, huni, stok ve rezervasyon rakamları. Tüm tutarlar EUR; tarihler UTC izler. Gerekli yerlerde CSV dışa aktarın.';
de['Sales, tax, shipping'] = 'Verkauf, Steuer, Versand';
ru['Sales, tax, shipping'] = 'Продажи, налог, доставка';
tr['Sales, tax, shipping'] = 'Satış, vergi, kargo';
de['Save article'] = 'Artikel speichern';
ru['Save article'] = 'Сохранить статью';
tr['Save article'] = 'Makaleyi kaydet';
de['Save changes'] = 'Änderungen speichern';
ru['Save changes'] = 'Сохранить изменения';
tr['Save changes'] = 'Değişiklikleri kaydet';
de['Save content'] = 'Inhalt speichern';
ru['Save content'] = 'Сохранить контент';
tr['Save content'] = 'İçeriği kaydet';
de['Save draft'] = 'Entwurf speichern';
ru['Save draft'] = 'Сохранить черновик';
tr['Save draft'] = 'Taslağı kaydet';
de['Save mapping'] = 'Zuordnung speichern';
ru['Save mapping'] = 'Сохранить сопоставление';
tr['Save mapping'] = 'Eşlemeyi kaydet';
de['Save settings'] = 'Einstellungen speichern';
ru['Save settings'] = 'Сохранить настройки';
tr['Save settings'] = 'Ayarları kaydet';
de['Save'] = 'Speichern';
ru['Save'] = 'Сохранить';
tr['Save'] = 'Kaydet';
de['SAVING'] = 'WIRD GESPEICHERT';
ru['SAVING'] = 'СОХРАНЕНИЕ';
tr['SAVING'] = 'KAYDEDİLİYOR';
de['Select a booking'] = 'Buchung auswählen';
ru['Select a booking'] = 'Выберите бронирование';
tr['Select a booking'] = 'Rezervasyon seçin';
de['Select a calendar'] = 'Kalender auswählen';
ru['Select a calendar'] = 'Выберите календарь';
tr['Select a calendar'] = 'Takvim seçin';
de['Select a form'] = 'Formular auswählen';
ru['Select a form'] = 'Выберите форму';
tr['Select a form'] = 'Form seçin';
de['Select an entry in the calendar to see the customer and synchronization details.'] =
  'Wählen Sie einen Eintrag im Kalender, um Kundendaten und Synchronisationsdetails zu sehen.';
ru['Select an entry in the calendar to see the customer and synchronization details.'] =
  'Выберите запись в календаре, чтобы увидеть данные клиента и детали синхронизации.';
tr['Select an entry in the calendar to see the customer and synchronization details.'] =
  'Müşteri ve senkronizasyon detaylarını görmek için takvimde bir kayıt seçin.';
de['Select an option'] = 'Option auswählen';
ru['Select an option'] = 'Выберите вариант';
tr['Select an option'] = 'Bir seçenek seçin';
de['Select an order to review items, payment, and actions.'] =
  'Wählen Sie eine Bestellung aus, um Artikel, Zahlung und Aktionen zu prüfen.';
ru['Select an order to review items, payment, and actions.'] =
  'Выберите заказ, чтобы просмотреть позиции, платёж и действия.';
tr['Select an order to review items, payment, and actions.'] =
  'Ürünleri, ödemeyi ve işlemleri incelemek için bir sipariş seçin.';
de['Select which storefront pages should display this article:'] =
  'Wählen Sie aus, auf welchen Storefront-Seiten dieser Artikel angezeigt werden soll:';
ru['Select which storefront pages should display this article:'] =
  'Выберите страницы витрины, на которых должна отображаться эта статья:';
tr['Select which storefront pages should display this article:'] =
  'Bu makalenin hangi vitrin sayfalarında görüneceğini seçin:';
de['Send bookings to Google'] = 'Buchungen an Google senden';
ru['Send bookings to Google'] = 'Отправлять бронирования в Google';
tr['Send bookings to Google'] = 'Rezervasyonları Googlea gönder';
de['SERVICE CATALOG'] = 'DIENSTKATALOG';
ru['SERVICE CATALOG'] = 'КАТАЛОГ УСЛУГ';
tr['SERVICE CATALOG'] = 'HİZMET KATALOĞU';
de['Service name'] = 'Dienstname';
ru['Service name'] = 'Название услуги';
tr['Service name'] = 'Hizmet adı';
de['Service status.'] = 'Dienstatus.';
ru['Service status.'] = 'Статус служб.';
tr['Service status.'] = 'Hizmet durumu.';
de['SERVICE STATUS'] = 'DIENSTSTATUS';
ru['SERVICE STATUS'] = 'СТАТУС СЛУЖБ';
tr['SERVICE STATUS'] = 'HİZMET DURUMU';
de['Service'] = 'Dienst';
ru['Service'] = 'Услуга';
tr['Service'] = 'Hizmet';
de['Services and duration'] = 'Dienste und Dauer';
ru['Services and duration'] = 'Услуги и длительность';
tr['Services and duration'] = 'Hizmetler ve süre';
de['Services'] = 'Dienste';
ru['Services'] = 'Услуги';
tr['Services'] = 'Hizmetler';
de['Set the appointment duration. Customers select the service before they select a time.'] =
  'Legen Sie die Terminsdauer fest. Kunden wählen den Dienst, bevor sie einen Zeitpunkt wählen.';
ru['Set the appointment duration. Customers select the service before they select a time.'] =
  'Задайте длительность визита. Клиенты выбирают услугу перед выбором времени.';
tr['Set the appointment duration. Customers select the service before they select a time.'] =
  'Randevu süresini belirleyin. Müşteriler önce hizmeti, sonra saati seçer.';
de['Settings saved.'] = 'Einstellungen gespeichert.';
ru['Settings saved.'] = 'Настройки сохранены.';
tr['Settings saved.'] = 'Ayarlar kaydedildi.';
de['Settings'] = 'Einstellungen';
ru['Settings'] = 'Настройки';
tr['Settings'] = 'Ayarlar';
de['SETUP'] = 'EINRICHTUNG';
ru['SETUP'] = 'НАСТРОЙКА';
tr['SETUP'] = 'KURULUM';
de['Shipment'] = 'Versand';
ru['Shipment'] = 'Отправка';
tr['Shipment'] = 'Sevkiyat';
de['shipped with carrier and tracking number; stock deducted.'] =
  'mit Spediteur und Sendungsnummer versendet; Bestand abgezogen.';
ru['shipped with carrier and tracking number; stock deducted.'] =
  'отправлено перевозчиком с номером отслеживания; запас списан.';
tr['shipped with carrier and tracking number; stock deducted.'] =
  'taşıyıcı ve takip numarasıyla gönderildi; stok düşüldü.';
de['SHIPPED'] = 'VERSANDT';
ru['SHIPPED'] = 'ОТПРАВЛЕНО';
tr['SHIPPED'] = 'GÖNDERİLDİ';
de['Shipping'] = 'Versand';
ru['Shipping'] = 'Доставка';
tr['Shipping'] = 'Kargo';
de['SHOP AND PAYMENTS'] = 'SHOP UND ZAHLUNGEN';
ru['SHOP AND PAYMENTS'] = 'МАГАЗИН И ПЛАТЕЖИ';
tr['SHOP AND PAYMENTS'] = 'MAĞAZA VE ÖDEMELER';
de['SHOP CLOSED'] = 'SHOP GESCHLOSSEN';
ru['SHOP CLOSED'] = 'МАГАЗИН ЗАКРЫТ';
tr['SHOP CLOSED'] = 'MAĞAZA KAPALI';
de['SHOP OPEN'] = 'SHOP OFFEN';
ru['SHOP OPEN'] = 'МАГАЗИН ОТКРЫТ';
tr['SHOP OPEN'] = 'MAĞAZA AÇIK';
de['Shop orders'] = 'Shop-Bestellungen';
ru['Shop orders'] = 'Заказы магазина';
tr['Shop orders'] = 'Mağaza siparişleri';
de['SHOP SETTINGS'] = 'SHOPEINSTELLUNGEN';
ru['SHOP SETTINGS'] = 'НАСТРОЙКИ МАГАЗИНА';
tr['SHOP SETTINGS'] = 'MAĞAZA AYARLARI';
de['Shop'] = 'Shop';
ru['Shop'] = 'Магазин';
tr['Shop'] = 'Mağaza';
de['Short text'] = 'Kurzer Text';
ru['Short text'] = 'Короткий текст';
tr['Short text'] = 'Kısa metin';
de['Sign in'] = 'Anmelden';
ru['Sign in'] = 'Войти';
tr['Sign in'] = 'Giriş yap';
de['Sign out'] = 'Abmelden';
ru['Sign out'] = 'Выйти';
tr['Sign out'] = 'Çıkış yap';
de['Signing in'] = 'Anmeldung läuft';
ru['Signing in'] = 'Вход выполняется';
tr['Signing in'] = 'Giriş yapılıyor';
de['SINGLE TYPE'] = 'EINZELTYP';
ru['SINGLE TYPE'] = 'ОДИНОЧНЫЙ ТИП';
tr['SINGLE TYPE'] = 'TEK TİP';
de['Sku'] = 'Steuernummer';
ru['Sku'] = 'Артикул';
tr['Sku'] = 'Stok kodu';
de['SKU'] = 'STEUERNUMMER';
ru['SKU'] = 'АРТИКУЛ';
tr['SKU'] = 'STOK KODU';
de['Slug'] = 'Slug';
ru['Slug'] = 'Слаг';
tr['Slug'] = 'Kimlik';
de['Small label above heading (optional)'] = 'Kleine Beschriftung über der Überschrift (optional)';
ru['Small label above heading (optional)'] = 'Маленькая метка над заголовком (необязательно)';
tr['Small label above heading (optional)'] = 'Başlığın üzerindeki küçük etiket (isteğe bağlı)';
de['Start writing your article here...'] = 'Beginnen Sie hier mit dem Schreiben Ihres Artikels…';
ru['Start writing your article here...'] = 'Начните писать статью здесь…';
tr['Start writing your article here...'] = 'Makalenizi burada yazmaya başlayın…';
de['Status'] = 'Status';
ru['Status'] = 'Статус';
tr['Status'] = 'Durum';
de['stock held, customer not yet paid.'] = 'Bestand reserviert, Kunde hat noch nicht bezahlt.';
ru['stock held, customer not yet paid.'] = 'запас зарезервирован, клиент ещё не оплатил.';
tr['stock held, customer not yet paid.'] = 'stok bloke edildi, müşteri henüz ödeme yapmadı.';
de['Stock'] = 'Bestand';
ru['Stock'] = 'Запас';
tr['Stock'] = 'Stok';
de['Storefront color palette'] = 'Farbpalette der Storefront';
ru['Storefront color palette'] = 'Цветовая палитра витрины';
tr['Storefront color palette'] = 'Vitrin renk paleti';
de['Storefront page preview'] = 'Vorschau der Storefront-Seite';
ru['Storefront page preview'] = 'Предпросмотр страницы витрины';
tr['Storefront page preview'] = 'Vitrin sayfası önizlemesi';
de['STOREFRONT PAGES'] = 'STOREFRONT-SEITEN';
ru['STOREFRONT PAGES'] = 'СТРАНИЦЫ ВИТРИНЫ';
tr['STOREFRONT PAGES'] = 'VİTRİN SAYFALARI';
de['STOREFRONT PRESENTATION'] = 'STOREFRONT-GESTALTUNG';
ru['STOREFRONT PRESENTATION'] = 'ОФОРМЛЕНИЕ ВИТРИНЫ';
tr['STOREFRONT PRESENTATION'] = 'VİTRİN SUNUMU';
de['STOREFRONT PREVIEW'] = 'STOREFRONT-VORSCHAU';
ru['STOREFRONT PREVIEW'] = 'ПРЕДПРОСМОТР ВИТРИНЫ';
tr['STOREFRONT PREVIEW'] = 'VİTRİN ÖNİZLEMESİ';
de['STOREFRONT'] = 'STOREFRONT';
ru['STOREFRONT'] = 'ВИТРИНА';
tr['STOREFRONT'] = 'VİTRİN';
de['Strapi applies the permissions of your administrator role.'] =
  'Strapi wendet die Berechtigungen Ihrer Administratorrolle an.';
ru['Strapi applies the permissions of your administrator role.'] =
  'Strapi применяет права вашей роли администратора.';
tr['Strapi applies the permissions of your administrator role.'] =
  'Strapi yönetici rolünüzün izinlerini uygular.';
de['Strapi applies your role permissions to this content.'] =
  'Strapi wendet Ihre Rollenberechtigungen auf diesen Inhalt an.';
ru['Strapi applies your role permissions to this content.'] =
  'Strapi применяет права вашей роли к этому контенту.';
tr['Strapi applies your role permissions to this content.'] =
  'Strapi rolünüzün izinlerini bu içeriğe uygular.';
de['Strapi content access'] = 'Zugriff auf Strapi-Inhalte';
ru['Strapi content access'] = 'Доступ к контенту Strapi';
tr['Strapi content access'] = 'Strapi içerik erişimi';
de['Strapi did not save the content. Check your role permissions and the field values.'] =
  'Strapi hat den Inhalt nicht gespeichert. Prüfen Sie Ihre Rollenberechtigungen und die Feldwerte.';
ru['Strapi did not save the content. Check your role permissions and the field values.'] =
  'Strapi не сохранил контент. Проверьте права вашей роли и значения полей.';
tr['Strapi did not save the content. Check your role permissions and the field values.'] =
  'Strapi içeriği kaydetmedi. Rol izinlerinizi ve alan değerlerini kontrol edin.';
de['STRIPE CONNECTED'] = 'STRIPE VERBUNDEN';
ru['STRIPE CONNECTED'] = 'STRIPE ПОДКЛЮЧЁН';
tr['STRIPE CONNECTED'] = 'STRIPE BAĞLI';
de['Submission ID'] = 'Einsendungs-ID';
ru['Submission ID'] = 'Идентификатор отправки';
tr['Submission ID'] = 'Gönderim kimliği';
de['SUBMISSION'] = 'EINSENDUNG';
ru['SUBMISSION'] = 'ОТПРАВКА';
tr['SUBMISSION'] = 'GÖNDERİM';
de['Submissions pages'] = 'Seiten der Einsendungen';
ru['Submissions pages'] = 'Страницы отправок';
tr['Submissions pages'] = 'Gönderim sayfaları';
de['Submissions'] = 'Einsendungen';
ru['Submissions'] = 'Отправки';
tr['Submissions'] = 'Gönderimler';
de['SUBMISSIONS'] = 'EINSENDUNGEN';
ru['SUBMISSIONS'] = 'ОТПРАВКИ';
tr['SUBMISSIONS'] = 'GÖNDERİMLER';
de['Submit button label'] = 'Beschriftung des Absende-Buttons';
ru['Submit button label'] = 'Метка кнопки отправки';
tr['Submit button label'] = 'Gönder butonu etiketi';
de['Submit'] = 'Absenden';
ru['Submit'] = 'Отправить';
tr['Submit'] = 'Gönder';
de['Submitted at'] = 'Gesendet am';
ru['Submitted at'] = 'Отправлено в';
tr['Submitted at'] = 'Gönderim zamanı';
de['Subtotal'] = 'Zwischensumme';
ru['Subtotal'] = 'Промежуточный итог';
tr['Subtotal'] = 'Ara toplam';
de['SUCCEEDED'] = 'ERFOLGREICH';
ru['SUCCEEDED'] = 'УСПЕШНО';
tr['SUCCEEDED'] = 'BAŞARILI';
de['Success message'] = 'Erfolgsnachricht';
ru['Success message'] = 'Сообщение об успехе';
tr['Success message'] = 'Başarı mesajı';
de['Sync now'] = 'Jetzt synchronisieren';
ru['Sync now'] = 'Синхронизировать сейчас';
tr['Sync now'] = 'Şimdi senkronize et';
de['Sync: {status} · Conflicts: {count}'] = 'Synchronisierung: {status} · Konflikte: {count}';
ru['Sync: {status} · Conflicts: {count}'] = 'Синхронизация: {status} · Конфликты: {count}';
tr['Sync: {status} · Conflicts: {count}'] = 'Senkronizasyon: {status} · Çakışma: {count}';
de['Synchronized'] = 'Synchronisiert';
ru['Synchronized'] = 'Синхронизировано';
tr['Synchronized'] = 'Senkronize edildi';
de['System checks'] = 'Systemprüfungen';
ru['System checks'] = 'Проверки системы';
tr['System checks'] = 'Sistem denetimleri';
de['System status'] = 'Systemstatus';
ru['System status'] = 'Статус системы';
tr['System status'] = 'Sistem durumu';
de['System summary'] = 'Systemübersicht';
ru['System summary'] = 'Сводка по системе';
tr['System summary'] = 'Sistem özeti';
de['SYSTEM'] = 'SYSTEM';
ru['SYSTEM'] = 'СИСТЕМА';
tr['SYSTEM'] = 'SİSTEM';
de['Tag this article to pages'] = 'Diesen Artikel Seiten zuordnen';
ru['Tag this article to pages'] = 'Привязать эту статью к страницам';
tr['Tag this article to pages'] = 'Bu makaleyi sayfalara etiketle';
de['Tax rate (%)'] = 'Steuersatz (%)';
ru['Tax rate (%)'] = 'Ставка налога (%)';
tr['Tax rate (%)'] = 'Vergi oranı (%)';
de['Example: 19'] = 'Beispiel: 19';
ru['Example: 19'] = 'Пример: 19';
tr['Example: 19'] = 'Örnek: 19';
de['Enter 19 for a 19% tax rate.'] = 'Geben Sie für einen Steuersatz von 19 % den Wert 19 ein.';
ru['Enter 19 for a 19% tax rate.'] = 'Для ставки налога 19% введите 19.';
tr['Enter 19 for a 19% tax rate.'] = '%19 vergi oranı için 19 girin.';
de['Tax'] = 'Steuer';
ru['Tax'] = 'Налог';
tr['Tax'] = 'Vergi';
de['Telephone number'] = 'Telefonnummer';
ru['Telephone number'] = 'Номер телефона';
tr['Telephone number'] = 'Telefon numarası';
de['Text Block'] = 'Textblock';
ru['Text Block'] = 'Текстовый блок';
tr['Text Block'] = 'Metin bloğu';
de['Thank you. Your form was submitted.'] = 'Vielen Dank. Ihr Formular wurde übermittelt.';
ru['Thank you. Your form was submitted.'] = 'Спасибо. Ваша форма отправлена.';
tr['Thank you. Your form was submitted.'] = 'Teşekkürler. Formunuz gönderildi.';
de['The app cannot verify the Google Calendar connection.'] =
  'Die App kann die Google-Kalenderverbindung nicht prüfen.';
ru['The app cannot verify the Google Calendar connection.'] =
  'Приложение не может проверить подключение к Google Календарю.';
tr['The app cannot verify the Google Calendar connection.'] =
  'Uygulama Google Takvim bağlantısını doğrulayamıyor.';
de['The app could not cancel the booking.'] = 'Die App konnte die Buchung nicht stornieren.';
ru['The app could not cancel the booking.'] = 'Приложение не смогло отменить бронирование.';
tr['The app could not cancel the booking.'] = 'Uygulama rezervasyonu iptal edemedi.';
de['The app could not disconnect the Google Calendar account.'] =
  'Die App konnte das Google-Kalenderkonto nicht trennen.';
ru['The app could not disconnect the Google Calendar account.'] =
  'Приложение не смогло отключить аккаунт Google Календаря.';
tr['The app could not disconnect the Google Calendar account.'] =
  'Uygulama Google Takvim hesabını koparamadı.';
de['The app could not load booking settings.'] =
  'Die App konnte die Buchungseinstellungen nicht laden.';
ru['The app could not load booking settings.'] =
  'Приложение не смогло загрузить настройки бронирования.';
tr['The app could not load booking settings.'] = 'Uygulama rezervasyon ayarlarını yükleyemedi.';
de['The app could not load the booking calendar.'] =
  'Die App konnte den Buchungskalender nicht laden.';
ru['The app could not load the booking calendar.'] =
  'Приложение не смогло загрузить календарь бронирований.';
tr['The app could not load the booking calendar.'] = 'Uygulama rezervasyon takvimini yükleyemedi.';
de['The app could not load this content.'] = 'Die App konnte diesen Inhalt nicht laden.';
ru['The app could not load this content.'] = 'Приложение не смогло загрузить этот контент.';
tr['The app could not load this content.'] = 'Uygulama bu içeriği yükleyemedi.';
de['The app could not remove the calendar mapping.'] =
  'Die App konnte die Kalenderzuordnung nicht entfernen.';
ru['The app could not remove the calendar mapping.'] =
  'Приложение не смогло удалить сопоставление календаря.';
tr['The app could not remove the calendar mapping.'] = 'Uygulama takvim eşlemesini kaldıramadı.';
de['The app could not save the calendar mapping.'] =
  'Die App konnte die Kalenderzuordnung nicht speichern.';
ru['The app could not save the calendar mapping.'] =
  'Приложение не смогло сохранить сопоставление календаря.';
tr['The app could not save the calendar mapping.'] = 'Uygulama takvim eşlemesini kaydedemedi.';
de['The business figures could not be loaded.'] =
  'Die Geschäftszahlen konnten nicht geladen werden.';
ru['The business figures could not be loaded.'] = 'Показатели бизнеса не удалось загрузить.';
tr['The business figures could not be loaded.'] = 'İş rakamları yüklenemedi.';
de['The calendar mapping was removed.'] = 'Die Kalenderzuordnung wurde entfernt.';
ru['The calendar mapping was removed.'] = 'Сопоставление календаря удалено.';
tr['The calendar mapping was removed.'] = 'Takvim eşlemesi kaldırıldı.';
de['The color palette is published to the storefront.'] =
  'Die Farbpalette ist auf der Storefront veröffentlicht.';
ru['The color palette is published to the storefront.'] =
  'Цветовая палитра опубликована на витрине.';
tr['The color palette is published to the storefront.'] = 'Renk paleti vitrine yayınlandı.';
de['The form could not be opened.'] = 'Das Formular konnte nicht geöffnet werden.';
ru['The form could not be opened.'] = 'Форму не удалось открыть.';
tr['The form could not be opened.'] = 'Form açılamadı.';
de['The form is available on the storefront.'] = 'Das Formular ist auf der Storefront verfügbar.';
ru['The form is available on the storefront.'] = 'Форма доступна на витрине.';
tr['The form is available on the storefront.'] = 'Form vitrinde kullanılabilir.';
de['The form is published but disabled.'] = 'Das Formular ist veröffentlicht, aber deaktiviert.';
ru['The form is published but disabled.'] = 'Форма опубликована, но отключена.';
tr['The form is published but disabled.'] = 'Form yayınlandı ama devre dışı.';
de['The form list could not be loaded.'] = 'Die Formularliste konnte nicht geladen werden.';
ru['The form list could not be loaded.'] = 'Список форм не удалось загрузить.';
tr['The form list could not be loaded.'] = 'Form listesi yüklenemedi.';
de['The Google Calendar account was disconnected.'] = 'Das Google-Kalenderkonto wurde getrennt.';
ru['The Google Calendar account was disconnected.'] = 'Аккаунт Google Календаря отключён.';
tr['The Google Calendar account was disconnected.'] = 'Google Takvim hesabı koptu.';
de['The order was cancelled and its stock hold released.'] =
  'Die Bestellung wurde storniert und ihre Bestandsreservierung freigegeben.';
ru['The order was cancelled and its stock hold released.'] =
  'Заказ отменён, резервирование запаса снято.';
tr['The order was cancelled and its stock hold released.'] =
  'Sipariş iptal edildi ve stok blokesi kaldırıldı.';
de['The order was fulfilled and stock deducted.'] =
  'Die Bestellung wurde ausgeführt und der Bestand abgezogen.';
ru['The order was fulfilled and stock deducted.'] = 'Заказ выполнен, запас списан.';
tr['The order was fulfilled and stock deducted.'] = 'Sipariş tamamlandı ve stok düşüldü.';
de['The order was marked paid.'] = 'Die Bestellung wurde als bezahlt markiert.';
ru['The order was marked paid.'] = 'Заказ отмечен как оплаченный.';
tr['The order was marked paid.'] = 'Sipariş ödendi olarak işaretlendi.';
de['The order was refunded and shipped items restocked.'] =
  'Die Bestellung wurde erstattet und versandte Artikel wieder in den Bestand aufgenommen.';
ru['The order was refunded and shipped items restocked.'] =
  'Заказ возвращён, отгруженные товары снова в запасе.';
tr['The order was refunded and shipped items restocked.'] =
  'Sipariş iade edildi ve gönderilen ürünler stoka geri kondu.';
de['The page "{slug}" does not exist yet. It will be created when you save.'] =
  'Die Seite „{slug}“ existiert noch nicht. Sie wird beim Speichern erstellt.';
ru['The page "{slug}" does not exist yet. It will be created when you save.'] =
  'Страница «{slug}» ещё не существует. Она будет создана при сохранении.';
tr['The page "{slug}" does not exist yet. It will be created when you save.'] =
  '"{slug}" sayfası henüz yok. Kaydettiğinizde oluşturulacak.';
de['The page is now live on the storefront.'] = 'Die Seite ist jetzt auf der Storefront aktiv.';
ru['The page is now live on the storefront.'] = 'Страница теперь опубликована на витрине.';
tr['The page is now live on the storefront.'] = 'Sayfa artık vitrinde yayında.';
de['The page is published but disabled on the storefront.'] =
  'Die Seite ist veröffentlicht, aber auf der Storefront deaktiviert.';
ru['The page is published but disabled on the storefront.'] =
  'Страница опубликована, но отключена на витрине.';
tr['The page is published but disabled on the storefront.'] =
  'Sayfa yayınlandı ama vitrinde devre dışı.';
de['The page list could not be loaded.'] = 'Die Seitenliste konnte nicht geladen werden.';
ru['The page list could not be loaded.'] = 'Список страниц не удалось загрузить.';
tr['The page list could not be loaded.'] = 'Sayfa listesi yüklenemedi.';
de['The preview uses the real storefront renderer.'] =
  'Die Vorschau nutzt den echten Storefront-Renderer.';
ru['The preview uses the real storefront renderer.'] =
  'Предпросмотр использует реальный рендерер витрины.';
tr['The preview uses the real storefront renderer.'] =
  'Önizleme gerçek vitrin rendercısını kullanır.';
de['The product was created.'] = 'Das Produkt wurde erstellt.';
ru['The product was created.'] = 'Товар создан.';
tr['The product was created.'] = 'Ürün oluşturuldu.';
de['The product was deleted.'] = 'Das Produkt wurde gelöscht.';
ru['The product was deleted.'] = 'Товар удалён.';
tr['The product was deleted.'] = 'Ürün silindi.';
de['The product was saved.'] = 'Das Produkt wurde gespeichert.';
ru['The product was saved.'] = 'Товар сохранён.';
tr['The product was saved.'] = 'Ürün kaydedildi.';
de['the provider-defined time'] = 'dem vom Anbieter definierten Zeitpunkt';
ru['the provider-defined time'] = 'определённое провайдером время';
tr['the provider-defined time'] = 'sağlayıcı tarafından belirlenen zaman';
de['The report request failed. Check the backend logs for details.'] =
  'Die Berichtsanfrage ist fehlgeschlagen. Prüfen Sie die Backend-Logs für Details.';
ru['The report request failed. Check the backend logs for details.'] =
  'Запрос отчёта не удался. Подробности — в журналах бэкенда.';
tr['The report request failed. Check the backend logs for details.'] =
  'Rapor isteği başarısız oldu. Detaylar için arka uç günlüklerini kontrol edin.';
de['The service could not be saved. Check its name and duration.'] =
  'Der Dienst konnte nicht gespeichert werden. Prüfen Sie seinen Namen und seine Dauer.';
ru['The service could not be saved. Check its name and duration.'] =
  'Услугу не удалось сохранить. Проверьте её название и длительность.';
tr['The service could not be saved. Check its name and duration.'] =
  'Hizmet kaydedilemedi. Adını ve süresini kontrol edin.';
de['The storefront preview address is not available.'] =
  'Die Storefront-Vorschau-Adresse ist nicht verfügbar.';
ru['The storefront preview address is not available.'] = 'Адрес предпросмотра витрины недоступен.';
tr['The storefront preview address is not available.'] = 'Vitrin önizleme adresi mevcut değil.';
de['The storefront settings could not be loaded.'] =
  'Die Storefront-Einstellungen konnten nicht geladen werden.';
ru['The storefront settings could not be loaded.'] = 'Настройки витрины не удалось загрузить.';
tr['The storefront settings could not be loaded.'] = 'Vitrin ayarları yüklenemedi.';
de['The submission was deleted.'] = 'Die Einsendung wurde gelöscht.';
ru['The submission was deleted.'] = 'Отправка удалена.';
tr['The submission was deleted.'] = 'Gönderim silindi.';
de['The website languages are saved.'] = 'Die Website-Sprachen sind gespeichert.';
ru['The website languages are saved.'] = 'Языки сайта сохранены.';
tr['The website languages are saved.'] = 'Site dilleri kaydedildi.';
de['this customer'] = 'diesem Kunden';
ru['this customer'] = 'этому клиенту';
tr['this customer'] = 'bu müşteriye';
de['This list separates working functions from planned functions.'] =
  'Diese Liste trennt funktionierende von geplanten Funktionen.';
ru['This list separates working functions from planned functions.'] =
  'Этот список разделяет работающие и запланированные функции.';
tr['This list separates working functions from planned functions.'] =
  'Bu liste çalışan işlevleri planlananlardan ayırır.';
de['This order could not be loaded.'] = 'Diese Bestellung konnte nicht geladen werden.';
ru['This order could not be loaded.'] = 'Этот заказ не удалось загрузить.';
tr['This order could not be loaded.'] = 'Bu sipariş yüklenemedi.';
de['This page could not be opened:'] = 'Diese Seite konnte nicht geöffnet werden:';
ru['This page could not be opened:'] = 'Эту страницу не удалось открыть:';
tr['This page could not be opened:'] = 'Bu sayfa açılamadı:';
de['This selection is saved in this browser and applies to the admin panel.'] =
  'Diese Auswahl wird in diesem Browser gespeichert und gilt für die Admin-Panel.';
ru['This selection is saved in this browser and applies to the admin panel.'] =
  'Этот выбор сохраняется в этом браузере и применяется к панели администратора.';
tr['This selection is saved in this browser and applies to the admin panel.'] =
  'Bu seçim bu tarayıcıda saklanır ve yönetici paneline uygulanır.';
de['Time zone'] = 'Zeitzone';
ru['Time zone'] = 'Часовой пояс';
tr['Time zone'] = 'Saat dilimi';
de['Time'] = 'Uhrzeit';
ru['Time'] = 'Время';
tr['Time'] = 'Saat';
de['TO'] = 'BIS';
ru['TO'] = 'ПО';
tr['TO'] = 'BİTİŞ';
de['Today'] = 'Heute';
ru['Today'] = 'Сегодня';
tr['Today'] = 'Bugün';
de['Toggle navigation'] = 'Navigation umschalten';
ru['Toggle navigation'] = 'Переключить навигацию';
tr['Toggle navigation'] = 'Navigasyonu aç/kapat';
de['TOP PRODUCTS'] = 'BESTE PRODUKTE';
ru['TOP PRODUCTS'] = 'ТОП ТОВАРОВ';
tr['TOP PRODUCTS'] = 'EN İYİ ÜRÜNLER';
de['Total'] = 'Gesamt';
ru['Total'] = 'Итого';
tr['Total'] = 'Toplam';
de['Track bookings, form submissions, and shop orders across the platform.'] =
  'Verfolgen Sie Buchungen, Formulareinsendungen und Shop-Bestellungen über die gesamte Plattform.';
ru['Track bookings, form submissions, and shop orders across the platform.'] =
  'Отслеживайте бронирования, отправки форм и заказы магазина по всей платформе.';
tr['Track bookings, form submissions, and shop orders across the platform.'] =
  'Tüm platformda rezervasyonları, form gönderimlerini ve mağaza siparişlerini izleyin.';
de['Tracking number'] = 'Sendungsnummer';
ru['Tracking number'] = 'Номер отслеживания';
tr['Tracking number'] = 'Takip numarası';
de['Unavailable'] = 'Nicht verfügbar';
ru['Unavailable'] = 'Недоступен';
tr['Unavailable'] = 'Kullanılamıyor';
de['Underline'] = 'Unterstreichen';
ru['Underline'] = 'Подчёркивание';
tr['Underline'] = 'Altı çizili';
de['UNITS SOLD'] = 'VERKAUFTE EINHEITEN';
ru['UNITS SOLD'] = 'ПРОДАНО ЕДИНИЦ';
tr['UNITS SOLD'] = 'SATILAN ADET';
de['Units'] = 'Einheiten';
ru['Units'] = 'Единицы';
tr['Units'] = 'Adet';
de['unpaid order closed, or paid order refunded and restocked.'] =
  'unbezahlte Bestellung geschlossen oder bezahlte Bestellung erstattet und nachbestockt.';
ru['unpaid order closed, or paid order refunded and restocked.'] =
  'неоплаченный заказ закрыт или оплаченный возвращён и снова в запасе.';
tr['unpaid order closed, or paid order refunded and restocked.'] =
  'ödenmemiş sipariş kapatıldı veya ödenmiş sipariş iade edilip stoka geri kondu.';
de['Unpublish'] = 'Veröffentlichung aufheben';
ru['Unpublish'] = 'Снять с публикации';
tr['Unpublish'] = 'Yayından kaldır';
de['Unsaved changes'] = 'Nicht gespeicherte Änderungen';
ru['Unsaved changes'] = 'Несохранённые изменения';
tr['Unsaved changes'] = 'Kaydedilmemiş değişiklikler';
de['Untitled article'] = 'Artikel ohne Titel';
ru['Untitled article'] = 'Статья без названия';
tr['Untitled article'] = 'Başlıksız makale';
de['Untitled field'] = 'Feld ohne Beschriftung';
ru['Untitled field'] = 'Поле без метки';
tr['Untitled field'] = 'Etiketsiz alan';
de['Untitled form'] = 'Formular ohne Namen';
ru['Untitled form'] = 'Форма без названия';
tr['Untitled form'] = 'Adsız form';
de['Use lowercase letters, numbers, and underscores.'] =
  'Verwenden Sie Kleinbuchstaben, Zahlen und Unterstriche.';
ru['Use lowercase letters, numbers, and underscores.'] =
  'Используйте строчные буквы, цифры и подчёркивания.';
tr['Use lowercase letters, numbers, and underscores.'] =
  'Küçük harfler, rakamlar ve alt çizgiler kullanın.';
de['Use your local Strapi administrator account.'] =
  'Verwenden Sie Ihr lokales Strapi-Administrator-Konto.';
ru['Use your local Strapi administrator account.'] =
  'Используйте локальную учётную запись администратора Strapi.';
tr['Use your local Strapi administrator account.'] = 'Yerel Strapi yönetici hesabınızı kullanın.';
de['Validation'] = 'Validierung';
ru['Validation'] = 'Проверка';
tr['Validation'] = 'Doğrulama';
de['Values appear here after customers send this form from the storefront.'] =
  'Werte erscheinen hier, nachdem Kunden dieses Formular von der Storefront senden.';
ru['Values appear here after customers send this form from the storefront.'] =
  'Значения появятся здесь после того, как клиенты отправят эту форму с витрины.';
tr['Values appear here after customers send this form from the storefront.'] =
  'Müşteriler bu formu vitrinden gönderdikten sonra değerler burada görünür.';
de['via'] = 'über';
ru['via'] = 'через';
tr['via'] = 'ile';
de['View'] = 'Ansehen';
ru['View'] = 'Просмотреть';
tr['View'] = 'Görüntüle';
de['Website and basic booking'] = 'Website und Basis-Buchung';
ru['Website and basic booking'] = 'Сайт и базовое бронирование';
tr['Website and basic booking'] = 'Web sitesi ve temel rezervasyon';
de['Website languages'] = 'Websitesprachen';
ru['Website languages'] = 'Языки сайта';
tr['Website languages'] = 'Site dilleri';
de['WEEK {number}'] = 'KWOCHE {number}';
ru['WEEK {number}'] = 'НЕДЕЛЯ {number}';
tr['WEEK {number}'] = '{number}. HAFTA';
de['WEEK AT A GLANCE'] = 'WOCHENÜBERSICHT';
ru['WEEK AT A GLANCE'] = 'НЕДЕЛЯ ВЗГЛЯДОМ';
tr['WEEK AT A GLANCE'] = 'HAFTAYA GENEL BAKIŞ';
de['Write articles once, tag them to pages:'] =
  'Schreiben Sie Artikel einmal und ordnen Sie sie Seiten zu:';
ru['Write articles once, tag them to pages:'] =
  'Напишите статьи один раз и привяжите их к страницам:';
tr['Write articles once, tag them to pages:'] =
  'Makaleleri bir kez yazın ve sayfalara etiketleyin:';
de['Write your text here.'] = 'Schreiben Sie hier Ihren Text.';
ru['Write your text here.'] = 'Напишите текст здесь.';
tr['Write your text here.'] = 'Metninizi buraya yazın.';
de['WYSIWYG'] = 'WYSIWYG';
ru['WYSIWYG'] = 'WYSIWYG';
tr['WYSIWYG'] = 'GÖRDÜĞÜN GİBİ YAYINLA';
de['Yes'] = 'Ja';
ru['Yes'] = 'Да';
tr['Yes'] = 'Evet';
de['Your administrator session expired. Sign in again to continue.'] =
  'Ihre Administrator-Session ist abgelaufen. Melden Sie sich erneut an, um fortzufahren.';
ru['Your administrator session expired. Sign in again to continue.'] =
  'Сессия администратора истекла. Войдите снова, чтобы продолжить.';
tr['Your administrator session expired. Sign in again to continue.'] =
  'Yönetici oturumunuz sona erdi. Devam etmek için tekrar giriş yapın.';
de['Your administrator session expired. Sign in again to view reports.'] =
  'Ihre Administrator-Session ist abgelaufen. Melden Sie sich erneut an, um Berichte zu sehen.';
ru['Your administrator session expired. Sign in again to view reports.'] =
  'Сессия администратора истекла. Войдите снова, чтобы посмотреть отчёты.';
tr['Your administrator session expired. Sign in again to view reports.'] =
  'Yönetici oturumunuz sona erdi. Raporları görmek için tekrar giriş yapın.';
de['Your Strapi role cannot open this content.'] =
  'Ihre Strapi-Rolle kann diesen Inhalt nicht öffnen.';
ru['Your Strapi role cannot open this content.'] =
  'Ваша роль Strapi не может открыть этот контент.';
tr['Your Strapi role cannot open this content.'] = 'Strapi rolünüz bu içeriği açamıyor.';
de['The database responded.'] = 'Die Datenbank hat geantwortet.';
ru['The database responded.'] = 'База данных отвечает.';
tr['The database responded.'] = 'Veritabanı yanıt verdi.';
de['Business setup is saved.'] = 'Die Geschäftseinstellung ist gespeichert.';
ru['Business setup is saved.'] = 'Настройка бизнеса сохранена.';
tr['Business setup is saved.'] = 'İş kurulumu kaydedildi.';
de['Save your business setup.'] = 'Speichern Sie Ihre Geschäftseinstellung.';
ru['Save your business setup.'] = 'Сохраните настройку бизнеса.';
tr['Save your business setup.'] = 'İş kurulumunuzu kaydedin.';
de['Google is not configured.'] = 'Google ist nicht konfiguriert.';
ru['Google is not configured.'] = 'Google не настроен.';
tr['Google is not configured.'] = 'Google yapılandırılmamış.';
de['Google client settings exist. Authorization is not verified.'] =
  'Die Google-Client-Einstellungen existieren. Die Autorisierung ist nicht geprüft.';
ru['Google client settings exist. Authorization is not verified.'] =
  'Настройки клиента Google существуют. Авторизация не проверена.';
tr['Google client settings exist. Authorization is not verified.'] =
  'Google istemci ayarları mevcut. Yetkilendirme doğrulanmadı.';
de['Stripe is not configured.'] = 'Stripe ist nicht konfiguriert.';
ru['Stripe is not configured.'] = 'Stripe не настроен.';
tr['Stripe is not configured.'] = 'Stripe yapılandırılmamış.';
de['Stripe settings exist. No payment test has run.'] =
  'Die Stripe-Einstellungen existieren. Kein Zahlungstest wurde ausgeführt.';
ru['Stripe settings exist. No payment test has run.'] =
  'Настройки Stripe существуют. Тест платежа не выполнялся.';
tr['Stripe settings exist. No payment test has run.'] =
  'Stripe ayarları mevcut. Ödeme testi çalıştırılmadı.';
de['Administrator sign-in is not configured.'] = 'Die Admin-Anmeldung ist nicht konfiguriert.';
ru['Administrator sign-in is not configured.'] = 'Вход администратора не настроен.';
tr['Administrator sign-in is not configured.'] = 'Yönetici girişi yapılandırılmamış.';
de['Strapi administrator settings exist. Sign-in is not verified.'] =
  'Die Strapi-Administrator-Einstellungen existieren. Die Anmeldung ist nicht geprüft.';
ru['Strapi administrator settings exist. Sign-in is not verified.'] =
  'Настройки администратора Strapi существуют. Вход не проверен.';
tr['Strapi administrator settings exist. Sign-in is not verified.'] =
  'Strapi yönetici ayarları mevcut. Giriş doğrulanmadı.';
de['Maison'] = 'Maison';
ru['Maison'] = 'Maison';
tr['Maison'] = 'Maison';
de['Warm cream surfaces with copper accents.'] = 'Warme cremefarbene Flächen mit Kupfer-Akzenten.';
ru['Warm cream surfaces with copper accents.'] = 'Тёплые кремовые поверхности с медными акцентами.';
tr['Warm cream surfaces with copper accents.'] = 'Bakır vurgulu sıcak krem yüzeyler.';
de['Noir Atelier'] = 'Noir Atelier';
ru['Noir Atelier'] = 'Noir Atelier';
tr['Noir Atelier'] = 'Noir Atelier';
de['Deep charcoal surfaces with gold-copper light.'] =
  'Tiefe anthrazitfarbene Flächen mit Gold-Kupfer-Licht.';
ru['Deep charcoal surfaces with gold-copper light.'] =
  'Глубокие угольные поверхности с золотисто-медным светом.';
tr['Deep charcoal surfaces with gold-copper light.'] = 'Altın-bakır ışıklı koyu antrasit yüzeyler.';
de['Lagoon'] = 'Lagoon';
ru['Lagoon'] = 'Lagoon';
tr['Lagoon'] = 'Lagoon';
de['Cool seafoam surfaces with deep teal accents.'] =
  'Kühle schaumfarbene Flächen mit tiefen Teal-Akzenten.';
ru['Cool seafoam surfaces with deep teal accents.'] =
  'Прохладные пенистые поверхности с глубокими бирюзовыми акцентами.';
tr['Cool seafoam surfaces with deep teal accents.'] =
  'Koyu turkuaz vurgulu serin köpük rengi yüzeyler.';
de['Blush'] = 'Blush';
ru['Blush'] = 'Blush';
tr['Blush'] = 'Blush';
de['Soft pink surfaces with raspberry accents.'] = 'Weiche pinke Flächen mit Himbeer-Akzenten.';
ru['Soft pink surfaces with raspberry accents.'] =
  'Мягкие розовые поверхности с малиновыми акцентами.';
tr['Soft pink surfaces with raspberry accents.'] = 'Frambuaz vurgulu yumuşak pembe yüzeyler.';
de['Overview'] = 'Übersicht';
ru['Overview'] = 'Обзор';
tr['Overview'] = 'Genel bakış';
de['Content'] = 'Inhalt';
ru['Content'] = 'Контент';
tr['Content'] = 'İçerik';
de['Resources'] = 'Ressourcen';
ru['Resources'] = 'Ресурсы';
tr['Resources'] = 'Kaynaklar';
de['Pages'] = 'Seiten';
ru['Pages'] = 'Страницы';
tr['Pages'] = 'Sayfalar';
de['Connections'] = 'Verbindungen';
ru['Connections'] = 'Подключения';
tr['Connections'] = 'Bağlantılar';
de['Commerce'] = 'Commerce';
ru['Commerce'] = 'Торговля';
tr['Commerce'] = 'Ticaret';
de['Setup'] = 'Einrichtung';
ru['Setup'] = 'Настройка';
tr['Setup'] = 'Kurulum';
de['Appearance'] = 'Erscheinungsbild';
ru['Appearance'] = 'Оформление';
tr['Appearance'] = 'Görünüm';
de['System'] = 'System';
ru['System'] = 'Система';
tr['System'] = 'Sistem';
de['Availability checks'] = 'Verfügbarkeitsprüfungen';
ru['Availability checks'] = 'Проверки доступности';
tr['Availability checks'] = 'Müsaitlik denetimleri';
de['Booking lifecycle'] = 'Buchungslebenszyklus';
ru['Booking lifecycle'] = 'Жизненный цикл бронирования';
tr['Booking lifecycle'] = 'Rezervasyon yaşam döngüsü';
de['Page and navigation editing'] = 'Bearbeitung von Seiten und Navigation';
ru['Page and navigation editing'] = 'Редактирование страниц и навигации';
tr['Page and navigation editing'] = 'Sayfa ve navigasyon düzenleme';
de['Resource calendar mapping'] = 'Kalenderzuordnung der Ressourcen';
ru['Resource calendar mapping'] = 'Сопоставление ресурсов с календарём';
tr['Resource calendar mapping'] = 'Kaynak takvim eşlemesi';
de['Google Calendar synchronization'] = 'Synchronisierung des Google-Kalenders';
ru['Google Calendar synchronization'] = 'Синхронизация Google Календаря';
tr['Google Calendar synchronization'] = 'Google Takvim senkronizasyonu';
de['Commerce and payments'] = 'Commerce und Zahlungen';
ru['Commerce and payments'] = 'Торговля и платежи';
tr['Commerce and payments'] = 'Ticaret ve ödemeler';
de['Monitoring and reports'] = 'Überwachung und Berichte';
ru['Monitoring and reports'] = 'Мониторинг и отчёты';
tr['Monitoring and reports'] = 'İzleme ve raporlar';
de['Spring backend'] = 'Spring-Backend';
ru['Spring backend'] = 'Бэкенд Spring';
tr['Spring backend'] = 'Spring arka uç';
de['Strapi CMS'] = 'Strapi-CMS';
ru['Strapi CMS'] = 'CMS Strapi';
tr['Strapi CMS'] = 'Strapi CMS';
de['Admin page editor'] = 'Admin-Seiten-Editor';
ru['Admin page editor'] = 'Редактор страниц администратора';
tr['Admin page editor'] = 'Yönetici sayfa düzenleyicisi';
de['Booking backend'] = 'Buchungs-Backend';
ru['Booking backend'] = 'Бэкенд бронирования';
tr['Booking backend'] = 'Rezervasyon arka uç';
de['Integration service'] = 'Integrationsdienst';
ru['Integration service'] = 'Служба интеграции';
tr['Integration service'] = 'Entegrasyon servisi';
de['Commerce backend'] = 'Commerce-Backend';
ru['Commerce backend'] = 'Бэкенд торговли';
tr['Commerce backend'] = 'Ticaret arka uç';
de['Reports panel + Prometheus metrics'] = 'Berichts-Panel + Prometheus-Metriken';
ru['Reports panel + Prometheus metrics'] = 'Панель отчётов + метрики Prometheus';
tr['Reports panel + Prometheus metrics'] = 'Rapor paneli + Prometheus metrikleri';
de['Custom palette'] = 'Eigene Farbpalette';
ru['Custom palette'] = 'Собственная палитра';
tr['Custom palette'] = 'Özel palet';
de['Choose each color yourself.'] = 'Wählen Sie jede Farbe selbst.';
ru['Choose each color yourself.'] = 'Выберите каждый цвет самостоятельно.';
tr['Choose each color yourself.'] = 'Her rengi kendiniz seçin.';
de['Save custom palette'] = 'Eigene Farbpalette speichern';
ru['Save custom palette'] = 'Сохранить собственную палитру';
tr['Save custom palette'] = 'Özel paleti kaydet';
de['Apply to admin panel'] = 'Auf Admin-Panel anwenden';
ru['Apply to admin panel'] = 'Применить к панели администратора';
tr['Apply to admin panel'] = 'Yönetici paneline uygula';
de['Select a color to start editing.'] =
  'Wählen Sie eine Farbe, um mit der Bearbeitung zu beginnen.';
ru['Select a color to start editing.'] = 'Выберите цвет, чтобы начать редактирование.';
tr['Select a color to start editing.'] = 'Düzenlemeye başlamak için bir renk seçin.';
de['The custom palette is published to the storefront.'] =
  'Die eigene Farbpalette ist auf der Storefront veröffentlicht.';
ru['The custom palette is published to the storefront.'] =
  'Собственная палитра опубликована на витрине.';
tr['The custom palette is published to the storefront.'] = 'Özel palet vitrine yayınlandı.';
de['The custom palette could not be saved.'] =
  'Die eigene Farbpalette konnte nicht gespeichert werden.';
ru['The custom palette could not be saved.'] = 'Собственную палитру не удалось сохранить.';
tr['The custom palette could not be saved.'] = 'Özel palet kaydedilemedi.';
de['Background'] = 'Hintergrund';
ru['Background'] = 'Фон';
tr['Background'] = 'Arka plan';
de['Surface'] = 'Fläche';
ru['Surface'] = 'Поверхность';
tr['Surface'] = 'Yüzey';
de['Accent'] = 'Akzent';
ru['Accent'] = 'Акцент';
tr['Accent'] = 'Vurgu';
de['Text'] = 'Text';
ru['Text'] = 'Текст';
tr['Text'] = 'Metin';
de['Sidebar'] = 'Seitenleiste';
ru['Sidebar'] = 'Боковая панель';
tr['Sidebar'] = 'Kenar çubuğu';
de['APPEARANCE'] = 'DESIGN';
ru['APPEARANCE'] = 'ОФОРМЛЕНИЕ';
tr['APPEARANCE'] = 'GÖRÜNÜM';
de[
  'Configure the color palette and available languages for your customer site and for this admin panel.'
] =
  'Konfigurieren Sie die Farbpalette und verfügbaren Sprachen für Ihre Kundenseite und für dieses Admin-Panel.';
ru[
  'Configure the color palette and available languages for your customer site and for this admin panel.'
] =
  'Настройте цветовую палитру и доступные языки для сайта клиентов и для этой панели администратора.';
tr[
  'Configure the color palette and available languages for your customer site and for this admin panel.'
] =
  'Müşteri siteniz ve bu yönetici paneli için renk paletini ve kullanılabilir dilleri yapılandırın.';
de['ADMIN PANEL'] = 'ADMIN-PANEL';
ru['ADMIN PANEL'] = 'ПАНЕЛЬ АДМИНИСТРАТОРА';
tr['ADMIN PANEL'] = 'YÖNETİCİ PANELİ';
de['Admin panel color palette'] = 'Farbpalette des Admin-Panels';
ru['Admin panel color palette'] = 'Цветовая палитра панели администратора';
tr['Admin panel color palette'] = 'Yönetici paneli renk paleti';
de['Admin interface languages'] = 'Sprachen der Benutzeroberfläche';
ru['Admin interface languages'] = 'Языки интерфейса';
tr['Admin interface languages'] = 'Arayüz dilleri';
de[
  'Enabled languages appear in the admin panel language switcher for every administrator. Keep at least one enabled.'
] =
  'Aktivierte Sprachen erscheinen im Sprachumschalter des Admin-Panels für jeden Administrator. Halten Sie mindestens eine Sprache aktiviert.';
ru[
  'Enabled languages appear in the admin panel language switcher for every administrator. Keep at least one enabled.'
] =
  'Включённые языки отображаются в переключателе языка панели администратора для каждого администратора. Оставьте хотя бы один язык включённым.';
tr[
  'Enabled languages appear in the admin panel language switcher for every administrator. Keep at least one enabled.'
] =
  'Etkin diller her yönetici için yönetici paneli dil değiştiricisinde görünür. En az bir dili etkin tutun.';
de['The admin interface languages could not be loaded.'] =
  'Die Sprachen der Benutzeroberfläche konnten nicht geladen werden.';
ru['The admin interface languages could not be loaded.'] = 'Не удалось загрузить языки интерфейса.';
tr['The admin interface languages could not be loaded.'] = 'Arayüz dilleri yüklenemedi.';
de['The admin interface languages are saved.'] =
  'Die Sprachen der Benutzeroberfläche sind gespeichert.';
ru['The admin interface languages are saved.'] = 'Языки интерфейса сохранены.';
tr['The admin interface languages are saved.'] = 'Arayüz dilleri kaydedildi.';
de['Loading…'] = 'Wird geladen…';
ru['Loading…'] = 'Загрузка…';
tr['Loading…'] = 'Yükleniyor…';
de['Appearance sections'] = 'Design-Bereiche';
ru['Appearance sections'] = 'Разделы оформления';
tr['Appearance sections'] = 'Görünüm bölümleri';
de['Storefront'] = 'Kundenseite';
ru['Storefront'] = 'Витрина';
tr['Storefront'] = 'Mağaza vitrini';
de['Admin panel'] = 'Admin-Panel';
ru['Admin panel'] = 'Панель администратора';
tr['Admin panel'] = 'Yönetici paneli';
de['Email'] = 'E-Mail';
ru['Email'] = 'Почта';
tr['Email'] = 'E-posta';
de['EMAIL'] = 'E-MAIL';
ru['EMAIL'] = 'ПОЧТА';
tr['EMAIL'] = 'E-POSTA';
de['Email settings'] = 'E-Mail-Einstellungen';
ru['Email settings'] = 'Настройки электронной почты';
tr['Email settings'] = 'E-posta ayarları';
de['Configure the subject lines and HTML templates for platform mail.'] =
  'Konfigurieren Sie die Betreffzeilen und HTML-Vorlagen für die Plattform-E-Mails.';
ru['Configure the subject lines and HTML templates for platform mail.'] =
  'Настройте строки тем и HTML-шаблоны для электронной почты платформы.';
tr['Configure the subject lines and HTML templates for platform mail.'] =
  'Platform e-postası için konu satırlarını ve HTML şablonlarını yapılandırın.';
de['PLATFORM'] = 'PLATTFORM';
ru['PLATFORM'] = 'ПЛАТФОРМА';
tr['PLATFORM'] = 'PLATFORM';
de['Form submission receipt'] = 'Eingangsbestätigung für Formulareinsendungen';
ru['Form submission receipt'] = 'Подтверждение получения отправки формы';
tr['Form submission receipt'] = 'Onay formu gönderimi';
de['RECEIPT TO SUBMITTER'] = 'BESTÄTIGUNG AN ABSENDER';
ru['RECEIPT TO SUBMITTER'] = 'ПОДТВЕРЖДЕНИЕ ОТПРАВИТЕЛЮ';
tr['RECEIPT TO SUBMITTER'] = 'GÖNDERENE ONAY';
de['Subject line'] = 'Betreffzeile';
ru['Subject line'] = 'Строка темы';
tr['Subject line'] = 'Konu satırı';
de['Template (HTML)'] = 'Vorlage (HTML)';
ru['Template (HTML)'] = 'Шаблон (HTML)';
tr['Template (HTML)'] = 'Şablon (HTML)';
de[
  'The submitter receives this mail after a form submission. Leave the template empty to use the built-in text.'
] =
  'Der Absender erhält diese E-Mail nach einer Formulareinsendung. Lassen Sie die Vorlage leer, um den eingebauten Text zu verwenden.';
ru[
  'The submitter receives this mail after a form submission. Leave the template empty to use the built-in text.'
] =
  'Отправитель получает это письмо после отправки формы. Оставьте шаблон пустым, чтобы использовать встроенный текст.';
tr[
  'The submitter receives this mail after a form submission. Leave the template empty to use the built-in text.'
] =
  'Gönderen bu e-postayı form gönderiminden sonra alır. Yerleşik metni kullanmak için şablonu boş bırakın.';
de['Order confirmation'] = 'Bestellbestätigung';
ru['Order confirmation'] = 'Подтверждение заказа';
tr['Order confirmation'] = 'Sipariş onayı';
de['TO CUSTOMER'] = 'AN KUNDEN';
ru['TO CUSTOMER'] = 'КЛИЕНТУ';
tr['TO CUSTOMER'] = 'MÜŞTERİYE';
de[
  'The customer receives this mail when the order payment is confirmed. Leave the template empty to use the built-in text.'
] =
  'Der Kunde erhält diese E-Mail, wenn die Bestellzahlung bestätigt ist. Lassen Sie die Vorlage leer, um den eingebauten Text zu verwenden.';
ru[
  'The customer receives this mail when the order payment is confirmed. Leave the template empty to use the built-in text.'
] =
  'Клиент получает это письмо после подтверждения оплаты заказа. Оставьте шаблон пустым, чтобы использовать встроенный текст.';
tr[
  'The customer receives this mail when the order payment is confirmed. Leave the template empty to use the built-in text.'
] =
  'Müşteri, sipariş ödemesi onaylandığında bu e-postayı alır. Yerleşik metni kullanmak için şablonu boş bırakın.';
de['Save mail settings'] = 'E-Mail-Einstellungen speichern';
ru['Save mail settings'] = 'Сохранить настройки почты';
tr['Save mail settings'] = 'E-posta ayarlarını kaydet';
de['Saving'] = 'Wird gespeichert';
ru['Saving'] = 'Сохранение';
tr['Saving'] = 'Kaydediliyor';
de['The mail settings were saved.'] = 'Die E-Mail-Einstellungen wurden gespeichert.';
ru['The mail settings were saved.'] = 'Настройки почты сохранены.';
tr['The mail settings were saved.'] = 'E-posta ayarları kaydedildi.';
de['The mail settings could not be loaded.'] =
  'Die E-Mail-Einstellungen konnten nicht geladen werden.';
ru['The mail settings could not be loaded.'] = 'Настройки почты не удалось загрузить.';
tr['The mail settings could not be loaded.'] = 'E-posta ayarları yüklenemedi.';
de['Submission receipt mail'] = 'Bestätigungs-E-Mail';
ru['Submission receipt mail'] = 'Письмо-подтверждение отправки';
tr['Submission receipt mail'] = 'Gönderim onay e-postası';
de['The submitter receives a confirmation mail. Choose which template it uses.'] =
  'Der Absender erhält eine Bestätigungs-E-Mail. Wählen Sie die verwendete Vorlage.';
ru['The submitter receives a confirmation mail. Choose which template it uses.'] =
  'Отправитель получает письмо-подтверждение. Выберите используемый шаблон.';
tr['The submitter receives a confirmation mail. Choose which template it uses.'] =
  'Gönderen bir onay e-postası alır. Kullanılacak şablonu seçin.';
de['Use the default template from Email settings'] =
  'Standard-Vorlage aus den E-Mail-Einstellungen verwenden';
ru['Use the default template from Email settings'] =
  'Использовать стандартный шаблон из настроек почты';
tr['Use the default template from Email settings'] =
  'E-posta ayarlarından varsayılan şablonu kullanın';
de['Use a custom template for this form'] = 'Eigene Vorlage für dieses Formular verwenden';
ru['Use a custom template for this form'] = 'Использовать собственный шаблон для этой формы';
tr['Use a custom template for this form'] = 'Bu form için özel şablon kullanın';
de['Save mail settings for this form'] = 'E-Mail-Vorlage für dieses Formular speichern';
ru['Save mail settings for this form'] = 'Сохранить шаблон почты для этой формы';
tr['Save mail settings for this form'] = 'Bu form için e-posta ayarlarını kaydet';
de['The mail settings for this form were saved.'] =
  'Die E-Mail-Einstellungen für dieses Formular wurden gespeichert.';
ru['The mail settings for this form were saved.'] = 'Настройки почты для этой формы сохранены.';
tr['The mail settings for this form were saved.'] = 'Bu form için e-posta ayarları kaydedildi.';
de['The mail settings could not be saved for this form.'] =
  'Die E-Mail-Einstellungen für dieses Formular konnten nicht gespeichert werden.';
ru['The mail settings could not be saved for this form.'] =
  'Настройки почты для этой формы не удалось сохранить.';
tr['The mail settings could not be saved for this form.'] =
  'Bu form için e-posta ayarları kaydedilemedi.';
de['Sample long text.'] = 'Beispielhafter Längstext.';
ru['Sample long text.'] = 'Пример длинного текста.';
tr['Sample long text.'] = 'Örnek uzun metin.';
de['Editor'] = 'Editor';
ru['Editor'] = 'Редактор';
tr['Editor'] = 'Düzenleyici';
de['Language'] = 'Sprache';
ru['Language'] = 'Язык';
tr['Language'] = 'Dil';
de['Default'] = 'Standard';
ru['Default'] = 'По умолчанию';
tr['Default'] = 'Varsayılan';
