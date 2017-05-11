if [ $# != 1 ]
then
	echo "Usage: create_new_rvi_security_files.sh output_dir"
	exit -1
fi

output_dir=$1
if [ ! -e "$output_dir" ]
then
	mkdir $output_dir
fi

rvica_prefix="rvica"
sslca_prefix="exoca"
root_prefix="root"
v1_prefix="v1"
v2_prefix="v2"
be_prefix='backend'
be_domain_name="genivi.org"

#ca_key=/home/lab/Documents/rvipoc/rvi/rvica/private/cakey.pem
#ca_cert=/home/lab/Documents/rvipoc/rvi/rvica/certs/cacert.pem
ca_key="$output_dir/$rvica_prefix"_key.pem
ca_cert="$output_dir/$rvica_prefix"_cert.pem
openssl_ca_certs_dir=/etc/ssl/certs


./rvi_create_credential.py --cred_out="$output_dir/$be_prefix"_credentials/"$be_prefix"_credential.json --jwt_out="$output_dir/$be_prefix"_credentials/"$be_prefix"_credential.jwt --id="$be_prefix"_abcd1 --issuer="$be_domain_name" --root_key="$output_dir/$root_prefix"_key.pem --device_cert="$output_dir/$be_prefix"_cert.pem --invoke="$be_domain_name/" --receive="$be_domain_name/backend/"
./rvi_create_credential.py --cred_out="$output_dir/$v1_prefix"_credentials/"$v1_prefix"_credential.json --jwt_out="$output_dir/$v1_prefix"_credentials/"$v1_prefix"_credential.jwt --id="$v1_prefix"_abcd2 --issuer="$be_domain_name" --root_key="$output_dir/$root_prefix"_key.pem --device_cert="$output_dir/$v1_prefix"_cert.pem --invoke="$be_domain_name/backend/logging/report" --receive="$be_domain_name/"
#./rvi_create_credential.py --cred_out="$output_dir/$v1_prefix"_credentials/"$v1_prefix"_credential.json --jwt_out="$output_dir/$v1_prefix"_credentials/"$v1_prefix"_credential.jwt --id="$v1_prefix"_abcd2 --issuer="$be_domain_name" --root_key="$output_dir/$root_prefix"_key.pem --device_cert="$output_dir/$v1_prefix"_cert.pem --invoke="$be_domain_name/backend/sota/initiate_download" --receive="$be_domain_name/"
./rvi_create_credential.py --cred_out="$output_dir/$v2_prefix"_credentials/"$v2_prefix"_credential.json --jwt_out="$output_dir/$v2_prefix"_credentials/"$v2_prefix"_credential.jwt --id="$v2_prefix"_abcd3 --issuer="$be_domain_name" --root_key="$output_dir/$root_prefix"_key.pem --device_cert="$output_dir/$v2_prefix"_cert.pem --invoke="$be_domain_name/backend/sota/initiate_download" --receive="$be_domain_name/"
exit 0


printf "FR\nPACA\nNice\nWorld Major CA\nCA BU\nJerome Cahuzac\njcahuzac@trust.fr\n\n\n" > "$output_dir/$rvica_prefix"_certinputdata.txt
printf "FR\nPACA\nValbonne\nWorld SSL Major CA\nCA BU\nFrancois Fillon\nffillon@trust.fr\n\n\n" > "$output_dir/$sslca_prefix"_certinputdata.txt
printf "FR\nParis\nParis\nSSL Corp\nSSL BU\nMichel\nmichel@sslcorp.com\n\n\n" > "$output_dir"/host_certinputdata.txt
printf "FR\nParis\nParis\nBestCorp\nConnected Car BU\nRoot\nroot@bestcorp.com\n\n\n" > "$output_dir/$root_prefix"_certinputdata.txt
printf "FR\nParis\nParis\nBestCorp\nConnected Car BU\nPatrick\npatrick@bestcorp.com\n\n\n" > "$output_dir/$be_prefix"_certinputdata.txt
printf "FR\nParis\nParis\nBestCorp\nConnected Car BU\nPatrick\npatrick@bestcorp.com\n\n\n" > "$output_dir/$v1_prefix"_certinputdata.txt
printf "FR\nParis\nParis\nBestCorp\nConnected Car BU\nPatrick\npatrick@bestcorp.com\n\n\n" > "$output_dir/$v2_prefix"_certinputdata.txt

# Create CA key and self-signed certificate
echo "##########################################################################"
echo "Create PKCS1 RVI CA key and cert, and install into /etc/ssl/certs/"
openssl genrsa -out "$output_dir/$rvica_prefix"_key.pem 1024
openssl req -new -key "$output_dir/$rvica_prefix"_key.pem -out "$output_dir/$rvica_prefix"_req.pem -nodes < "$output_dir/$rvica_prefix"_certinputdata.txt
openssl x509 -req -in "$output_dir/$rvica_prefix"_req.pem -sha256 -extensions v3_ca -signkey "$output_dir/$rvica_prefix"_key.pem -out "$output_dir/$rvica_prefix"_cert.pem
# Install new CA
sudo cp "$output_dir/$rvica_prefix"_cert.pem $openssl_ca_certs_dir
cd $openssl_ca_certs_dir
sudo ln -s "$rvica_prefix"_cert.pem `openssl x509 -hash -noout -in "$rvica_prefix"_cert.pem`.0
openssl verify -CApath $openssl_ca_certs_dir "$rvica_prefix"_cert.pem
cd -

# Create SSL key and certificate for exo library, to be installed in deps/exo/priv/
echo "##########################################################################"
echo "Create SHA1 CA key and cert for SSL exo lib, and install into /etc/ssl/certs/"
openssl req -newkey rsa:1024 -sha1 -keyout "$output_dir/$sslca_prefix"_key.pem -out "$output_dir/$sslca_prefix"_req.pem -nodes < "$output_dir/$sslca_prefix"_certinputdata.txt
openssl x509 -req -in "$output_dir/$sslca_prefix"_req.pem -sha1 -extensions v3_ca -signkey "$output_dir/$sslca_prefix"_key.pem -out "$output_dir/$sslca_prefix"_cert.pem
# Install new CA
sudo cp "$output_dir/$sslca_prefix"_cert.pem $openssl_ca_certs_dir
cd $openssl_ca_certs_dir
sudo ln -s "$sslca_prefix"_cert.pem `openssl x509 -hash -noout -in "$sslca_prefix"_cert.pem`.0
openssl verify -CApath $openssl_ca_certs_dir "$sslca_prefix"_cert.pem
cd -

echo "Create SHA1 key and certificate for SSL exo library, and install into ../deps/exo/priv/"
openssl req -newkey rsa:1024 -sha1 -keyout "$output_dir"/host.key -out "$output_dir"/host.cert_req -nodes < "$output_dir"/host_certinputdata.txt
openssl x509 -req -in "$output_dir"/host.cert_req -sha1 -extensions usr_cert -CA "$output_dir/$sslca_prefix"_cert.pem -CAkey "$output_dir/$sslca_prefix"_key.pem -CAcreateserial -out "$output_dir"/host.cert
# Print certificate
#openssl x509 -in "$output_dir"/host.cert -text -noout
echo "Verify SSL certificate versus CA key"
openssl verify -CAfile "$output_dir/$sslca_prefix"_cert.pem "$output_dir"/host.cert
cp "$output_dir"/host.* ../deps/exo/priv/

# Create RVI Root key and certificate signed by CA
echo "##########################################################################"
echo "Create new RVI root key => Warning: value of O (Organization) parameter should be different between Root and non-root keys to avoid self-signed error"
openssl genrsa -out "$output_dir/$root_prefix"_key.pem 1024
echo "##########################################################################"
echo "Create new RVI root cert"
openssl req -x509 -new -days 365 -key "$output_dir/$root_prefix"_key.pem -out "$output_dir/$root_prefix"_cert.pem -nodes < "$output_dir/$root_prefix"_certinputdata.txt
#openssl req -new -key "$output_dir/$root_prefix"_key.pem -out "$output_dir/$root_prefix"_req.pem -nodes < "$output_dir/$root_prefix"_certinputdata.txt
#openssl x509 -req -in "$output_dir/$root_prefix"_req.pem -sha256 -extensions usr_cert -CA $ca_cert -CAkey $ca_key -CAcreateserial -out "$output_dir/$root_prefix"_cert.pem
rm "$output_dir/$root_prefix"_req.pem

# Create Backend server key and certificate signed by CA
echo "##########################################################################"
echo "Create new key for backend node"
openssl genrsa -out "$output_dir/$be_prefix"_key.pem 1024
openssl req -new -key "$output_dir/$be_prefix"_key.pem -out "$output_dir/$be_prefix"_req.pem -nodes < "$output_dir/$be_prefix"_certinputdata.txt

echo "##########################################################################"
echo "Create new cert for backend node"
openssl x509 -req -days 365 -set_serial 01 -in "$output_dir/$be_prefix"_req.pem -sha256 -extensions usr_cert -CAkey "$output_dir/$root_prefix"_key.pem -CA "$output_dir/$root_prefix"_cert.pem -CAcreateserial -out "$output_dir/$be_prefix"_cert.pem
rm "$output_dir/$be_prefix"_req.pem

echo "##########################################################################"
echo "Verify $be_prefix certificate versus root key"
openssl verify -CAfile "$output_dir/$root_prefix"_cert.pem "$output_dir/$be_prefix"_cert.pem

echo "##########################################################################"
echo "Create new credential for $be_prefix"
mkdir "$output_dir/$be_prefix"_credentials
./rvi_create_credential.py --cred_out="$output_dir/$be_prefix"_credentials/"$be_prefix"_credential.json --jwt_out="$output_dir/$be_prefix"_credentials/"$be_prefix"_credential.jwt --id="$be_prefix"_abcd --issuer="$be_domain_name" --root_key="$output_dir/$root_prefix"_key.pem --device_cert="$output_dir/$be_prefix"_cert.pem --invoke="$be_domain_name/" --receive="$be_domain_name/"

# Create Vehicule 1 key and certificate signed by CA
echo "##########################################################################"
echo "Create new key for $v1_prefix node"
openssl genrsa -out "$output_dir/$v1_prefix"_key.pem 1024
openssl req -new -key "$output_dir/$v1_prefix"_key.pem -out "$output_dir/$v1_prefix"_req.pem -nodes < "$output_dir/$v1_prefix"_certinputdata.txt
echo "##########################################################################"
echo "Create new cert for $v1_prefix node"
openssl x509 -req -days 365 -set_serial 01 -in "$output_dir/$v1_prefix"_req.pem -sha256 -extensions usr_cert -CAkey "$output_dir/$root_prefix"_key.pem -CA "$output_dir/$root_prefix"_cert.pem -CAcreateserial -out "$output_dir/$v1_prefix"_cert.pem
rm "$output_dir/$v1_prefix"_req.pem

echo "##########################################################################"
echo "Verify $v1_prefix certificate versus root key"
openssl verify -CAfile "$output_dir/$root_prefix"_cert.pem "$output_dir/$v1_prefix"_cert.pem

echo "##########################################################################"
echo "Create new credential for $v1_prefix node"
mkdir "$output_dir/$v1_prefix"_credentials
./rvi_create_credential.py --cred_out="$output_dir/$v1_prefix"_credentials/"$v1_prefix"_credential.json --jwt_out="$output_dir/$v1_prefix"_credentials/"$v1_prefix"_credential.jwt --id="$v1_prefix"_abcd --issuer="$be_domain_name" --root_key="$output_dir/$root_prefix"_key.pem --device_cert="$output_dir/$v1_prefix"_cert.pem --invoke="$be_domain_name/" --receive="$be_domain_name/"

# Create Vehicule 2 key and certificate signed by CA
echo "##########################################################################"
echo "Create new key for $v2_prefix node"
openssl genrsa -out "$output_dir/$v2_prefix"_key.pem 1024
openssl req -new -key "$output_dir/$v2_prefix"_key.pem -out "$output_dir/$v2_prefix"_req.pem -nodes < "$output_dir/$v2_prefix"_certinputdata.txt
echo "##########################################################################"
echo "Create new cert for $v2_prefix node"
openssl x509 -req -days 365 -set_serial 01 -in "$output_dir/$v2_prefix"_req.pem -sha256 -extensions usr_cert -CAkey "$output_dir/$root_prefix"_key.pem -CA "$output_dir/$root_prefix"_cert.pem -CAcreateserial -out "$output_dir/$v2_prefix"_cert.pem
rm "$output_dir/$v2_prefix"_req.pem

echo "##########################################################################"
echo "Verify $v2_prefix certificate versus root key"
openssl verify -CAfile "$output_dir/$root_prefix"_cert.pem "$output_dir/$v2_prefix"_cert.pem

echo "##########################################################################"
echo "Create new credential for $v2_prefix node"
mkdir "$output_dir/$v2_prefix"_credentials
./rvi_create_credential.py --cred_out="$output_dir/$v2_prefix"_credentials/"$v2_prefix"_credential.json --jwt_out="$output_dir/$v2_prefix"_credentials/"$v2_prefix"_credential.jwt --id="$v2_prefix"_abcd --issuer="$be_domain_name" --root_key="$output_dir/$root_prefix"_key.pem --device_cert="$output_dir/$v2_prefix"_cert.pem --invoke="$be_domain_name/" --receive="$be_domain_name/"

