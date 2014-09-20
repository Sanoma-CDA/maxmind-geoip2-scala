echo Downloading GeoLite2-City.mmdb.gz
curl -O http://geolite.maxmind.com/download/geoip/database/GeoLite2-City.mmdb.gz
echo Gunzipping the DB file
gunzip GeoLite2-City.mmdb.gz
echo Note: some of the tests might fail if the contents of the DB has changed
