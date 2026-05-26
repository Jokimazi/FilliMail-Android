<div align="center">
  <img src="./images/fillimail_logo.png" alt="FilliMail Logo" width="120" height="120" />

# 📧 FilliMail

**Легковесный, быстрый и современный почтовый клиент для Android.**

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Version](https://img.shields.io/badge/version-1.0.0-blue?style=for-the-badge)

</div>

## 📖 О проекте

**FilliMail** — это нативное Android-приложение для удобного управления электронной почтой. Оно позволяет подключать неограниченное количество почтовых ящиков, работать с вложениями, читать письма в формате HTML и получать фоновые уведомления о новых сообщениях. Проект создан с упором на скорость работы, минимализм и следование стандартам Material Design.

## ✨ Основные возможности

* 👥 **Мультиаккаунтность:** Добавление нескольких почтовых ящиков и удобное переключение между ними через боковое меню (Navigation Drawer).
* 🔄 **IMAP & SMTP:** Полноценная работа с протоколами для чтения (INBOX, Sent, Trash) и отправки писем.
* 📎 **Вложения:** Поддержка прикрепления файлов к новым письмам и безопасное скачивание вложенных файлов из входящих писем напрямую в папку `Downloads`.
* 🎨 **Темы оформления:** Поддержка светлой, тёмной и системной темы оформления приложения.
* 🌍 **Локализация:** Полная поддержка русского и английского языков.
* 🔔 **Фоновые уведомления:** Автоматическая проверка новых писем в фоновом режиме (через `WorkManager`) без повышенного расхода батареи.
* 📝 **Поддержка HTML:** Корректное отображение сложных писем с разметкой, картинками и ссылками с адаптацией под мобильные экраны.

## 📱 Скриншоты

<div align="center">
  <img src="./images/fillimail_logo.png" alt="Login" width="250"/>
  <img src="./images/fillimail_logo.png" alt="Mailbox" width="250"/>
  <img src="./images/fillimail_logo.png" alt="Read Email" width="250"/>
</div>

## 🛠 Технологический стек

Приложение разработано с использованием современного стека технологий Android:

* **Язык:** Java 17
* **UI/UX:** XML Layouts, Material Components (`MaterialToolbar`, `NavigationView`, `TextInputLayout`)
* **Архитектура и данные:**
    * `Room Database` — для безопасного локального хранения учетных записей.
    * `JavaMail API (com.sun.mail)` — для сетевого взаимодействия по протоколам IMAP и SMTP.
    * `WorkManager` — для выполнения фоновых задач (уведомления).
* **Библиотеки:**
    * `Glide` — для асинхронной загрузки аватаров (Gravatar).
    * `SwipeRefreshLayout` — для обновления списков свайпом.

## 🚀 Установка и сборка

1. Склонируйте репозиторий:
   ```bash
   git clone [https://github.com/jokimazi/FilliMail-Android.git](https://github.com/jokimazi/FilliMail-Android.git)