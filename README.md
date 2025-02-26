## Облачное хранилище
### Структура
Структура работы описана в docker-compose.yml файле.  
Приложение запускает три контейнера:  
1. База данных postgreSQL, в которой хранятся данные пользователей и информация о файлах
2. Backend приложения, rest-сервис, в котором прописана логика обработки запросов по определенным эндпоинтам
3. Frontend, предоставляющий визуальную составляющую для работы с приложением.
### Возможности
Приложение предоставляет пользователю возможность   
- Сохранять
- Удалять
- Скачивать
- Переименовывать
- Сортировать
- Выводить список доступных файлов
  после успешной аутентификации.
### Аутентификация
Доступ к приложению открывается после успешной аутентификации. При обнаружении пользователя в базе и  
подтверждении его пароля, на сервер передается токен, который используется в дальнейшем при каждом запросе  
вплоть до выхода пользователя из аккаунта.
