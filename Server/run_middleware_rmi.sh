./run_rmi.sh > /dev/null 2>&1

# echo "Edit file run_middleware_rmi.sh to include instructions for launching the middleware"
# echo '  $1 - hostname of Flights'
# echo '  $2 - hostname of Cars'
# echo '  $3 - hostname of Rooms'

java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$(pwd)/ Server.RMI.RMIMiddleware $1 $2 $3
