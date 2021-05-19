echo Downloading GeoLite2-City.mmdb
aws s3 cp s3://seekingalpha-data/accessories/maxmind/GeoLite2-City.mmdb ./src/test/resources/GeoLite2-City.mmdb
echo Note: some of the tests might fail if the contents of the DB has changed