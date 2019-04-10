client-keystore.jks
client-truststore.jks
oasis.jks
server-keystore.jks
server-truststore.jks
sts-keystore.jks
sts-truststore.jks

You need to generate new certificates. The old ones were expired in the time of removal.


keytool -keystore client-keystore.jks -genkey -alias alice
keytool -keystore server-keystore.jks -genkey -alias bob
keytool -keystore sts-keystore.jks -genkey -alias wssip

keytool -export -keystore client-keystore.jks -alias alice -file Alice.cer
keytool -export -keystore server-keystore.jks -alias bob -file Bob.cer
keytool -export -keystore sts-keystore.jks -alias wssip -file Wssip.cer

keytool -import -file Bob.cer -alias bob -keystore client-truststore.jks
keytool -import -file Alice.cer -alias alice -keystore server-truststore.jks
keytool -import -file Wssip.cer -alias wssip -keystore client-truststore.jks
keytool -import -file Wssip.cer -alias wssip -keystore sts-truststore.jks
#keytool -import -file Wssip.cer -alias wssip -keystore server-truststore.jks
 