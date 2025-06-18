<?php
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type");
header("Access-Control-Allow-Credentials: true");

include "connect.php";
$status = false;

if (!isset($_REQUEST["chatID"])) {
    $data = ["error" => "Некорректный запрос"];
} else {
    $chatID = $_REQUEST["chatID"];
    $stmt = $db->prepare("SELECT * FROM punish_logs WHERE chatID = ?");

    $stmt->bind_param("s", $chatID);
    $stmt->execute() or die("Не удалось обработать запрос");
    $result = $stmt->get_result();
    
    $data = [];

    while ($logs = mysqli_fetch_array($result)) {
       $log = [
           'target' => $logs['targetID'],
           'type' => $logs['type'],
           'reason' => $logs['reason'],
           'date' => $logs['date']
           ];
           
        array_push($data, $log);
    }
    
    $status = true;
}

$answer = [
    "status" => $status,
    "data" => $data,
];

echo json_encode($answer, JSON_UNESCAPED_UNICODE);
?>
