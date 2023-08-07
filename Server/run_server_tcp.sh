#Usage: ./run_server_rmi.sh [<rmi_name>]

# ./run_rmi.sh > /dev/null 2>&1
java Server.TCP.TCPResourceManager $1
