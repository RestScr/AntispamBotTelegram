<?php
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type");
header("Access-Control-Allow-Credentials: true");

header("Content-Type: application/json");

include "connect.php";
$status = false;

if (!isset($_REQUEST["token"])) {
    $data = ["error" => "Некорректный запрос"];
} else {
    $token = $_REQUEST["token"];

    $stmt = $db->prepare("SELECT * FROM auth_tokens WHERE token = ?");
    $stmt->bind_param("s", $token);
    $stmt->execute() or die("Не удалось обработать запрос");
    $result = $stmt->get_result();

    if ($sessionData = mysqli_fetch_array($result)) {
        $status = true;
    }
}

//$status = true;
$answer = [
    "status" => $status,
    "data" => $data,
];

echo json_encode($answer, JSON_UNESCAPED_UNICODE);
?>