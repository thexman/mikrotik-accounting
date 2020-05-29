# mikrotik-accounting
Java service for uploading mikrotik traffic (accounting) information into InfluxDB

# Usage

```
Usage: java -jar mikrotik-accounting-1.0-SNAPSHOT-jar-with-dependencies.jar [options]
  Options:
    --console, -c
      Console mode
      Default: false
  * --db-name, -db
      Database name
  * --db-password, -p
      Database password
  * --db-url, -d
      Database URL (e.g. http://192.168.1.1:8086)
  * --db-user, -u
      Database user
    --help, -h

  * --router-ip, -r
      Router IP addres
  * --subnet, -n
      LAN subnets (e.g. 192.168.1.0/24)
      Default: []
```


## Example

```
java -jar mikrotik-accounting-1.0-SNAPSHOT-jar-with-dependencies.jar -r 192.168.88.1 -n 192.168.88.0/24 -d http://localhost:8086 -db TrafficByIp -u traffic -p secrerpassword
```

## Service
Copy `src/main/resources/mikrotik-accounting.service` to `/etc/systemd/system/mikrotik-accounting.service` and modify the java parameters (router ip, database url etc.)

Start the service 

```
sudo systemctl start mikrotik-accounting.service
```

Enable auto start on boot

```
sudo systemctl enable mikrotik-accounting.service
```


