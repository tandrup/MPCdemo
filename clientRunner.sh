#!/bin/bash
#

client[1]=scapa
client[2]=springbank
client[3]=oban
client[4]=rosebank

locate -m ${client[1]} ${client[2]} ${client[3]} ${client[4]}

run() {
    echo "Running with mCount = $1"

    ssh ${client[4]} java dk/au/daimi/tandrup/MPC/protocols/ClientTest server4.store server4 ${client[1]} server1 ${client[2]} server2 ${client[3]} server3 $1 &> client04_$1.log &
    
    ssh ${client[3]} java dk/au/daimi/tandrup/MPC/protocols/ClientTest server3.store server3 ${client[1]} server1 ${client[2]} server2 ${client[4]} server4 $1 &> client03_$1.log &
    
    ssh ${client[2]} java dk/au/daimi/tandrup/MPC/protocols/ClientTest server2.store server2 ${client[1]} server1 ${client[3]} server3 ${client[4]} server4 $1 &> client02_$1.log &
    
    ssh ${client[1]} java dk/au/daimi/tandrup/MPC/protocols/ClientTest server1.store server1 ${client[2]} server2 ${client[3]} server3 ${client[4]} server4 $1 &> client01_$1.log &

    echo "Waiting for jobs with mCount = $1 to finish"
    wait
}

run 4
run 10
run 16
run 20
run 24
run 32
run 40
