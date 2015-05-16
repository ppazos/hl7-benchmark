# hl7-benchmark

Sends messages in parallel to an HL7 server (like Mirth Connect or HAPI Test Panel) using MLLP or SOAP protocols, and throws a report of times consumed for each parallel thread and totals.

## Sample call

\hl7-benchmark > run -c 5 -n 100 -s localhost:2617 -t mllp -path sample_messages\OML_O21_1.hl7

## Sample report

    total plan 2273 ms
    plan media: 1246.5 ms
    plan min: 370 ms
    plan max: 2123 ms
    errors: 0
    ----------------------
    total plan 2734 ms
    plan media: 1892.5 ms
    plan min: 1051 ms
    plan max: 2734 ms
    errors: 0
    ----------------------
    total plan 3235 ms
    plan media: 1663 ms
    plan min: 271 ms
    plan max: 3055 ms
    errors: 0
    ----------------------
    total plan 2854 ms
    plan media: 1827.5 ms
    plan min: 811 ms
    plan max: 2844 ms
    errors: 0
    ----------------------
    total plan 3134 ms
    plan media: 1977.5 ms
    plan min: 1071 ms
    plan max: 2884 ms
    errors: 0
    ----------------------
    Total: 14230 ms
    Percentage of transactions completed in certain time (ms)
            16.6%   < 549.4
            6.6%    < 827.8
            9.0%    < 1106.2
            12.4%   < 1384.6
            9.8%    < 1663.0
            11.4%   < 1941.4
            12.4%   < 2219.8
            8.2%    < 2498.2
            9.4%    < 2776.6
            4.2%    < 3055.0
    
    
