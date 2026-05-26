<!-- FakeStoreAPI -->
<div align="center">
  <h3 align="center">GeoTrackingService</h3>
</div>


## Перед тем, как запустить

Должны быть установлены Docker, Git и Oracle 11g с готовой БД.

Для скачивания проекта на свой компьютер, в терминале git введите команду:
<ul>
    <li><code>git clone git@github.com:G-Man97/FileContentFilteringUtility.git</code> </li>
    <li>или <code>git clone https://github.com/G-Man97/FileContentFilteringUtility.git</code></li>
</ul>

Перейдите в терминале в папку с проектом и выполните команду <code>docker build -t geotrackingservice:1.0 .</code>
После создания образа запустите контейнер командой Docker'а и передайте переменные среды <code>DB_URL</code>, <code>DB_PASSWORD</code> 
и <code>DB_USERNAME</code> - настройки подключения к базе данных.

Например:
<code>docker run -d --name geotracking-app -e DB_URL="jdbc:oracle:thin:@host.docker.internal:1522/orcl" -e DB_USERNAME="SYSTEM" -e DB_PASSWORD="oracle" geotrackingservice:1.0</code>

### Built With

- Spring Boot
- Spring Data JPA
- Lombok
