# Usage: ./run_client_rmi.sh [<server_hostname> [<server_rmiobject>]]

java -cp ../Server/RMIInterface.jar:. Client.TCPClient $1 $2
