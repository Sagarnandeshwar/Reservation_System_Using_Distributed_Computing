::TODO: SPECIFY THE HOSTNAMES OF 4 CS MACHINES (lab2-8, lab2-10, etc...)
set MACHINES[0]="name1"
set MACHINES[1]="name2"
set MACHINES[2]="name3"
set MACHINES[3]="name4"

:: ok i give up on converting this file


:: tmux new-session \; \
:: 	split-window -h \; \
:: 	split-window -v \; \
:: 	split-window -v \; \
:: 	select-layout main-vertical \; \
:: 	select-pane -t 1 \; \
:: 	send-keys "ssh -t ${MACHINES[0]} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; ./run_server.sh Flights\"" C-m \; \
:: 	select-pane -t 2 \; \
:: 	send-keys "ssh -t ${MACHINES[1]} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; ./run_server.sh Cars\"" C-m \; \
:: 	select-pane -t 3 \; \
:: 	send-keys "ssh -t ${MACHINES[2]} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; ./run_server.sh Rooms\"" C-m \; \
:: 	select-pane -t 0 \; \
:: 	send-keys "ssh -t ${MACHINES[3]} \"cd $(pwd) > /dev/null; echo -n 'Connected to '; hostname; sleep .5s; ./run_middleware.sh ${MACHINES[0]} ${MACHINES[1]} ${MACHINES[2]}\"" C-m \;


