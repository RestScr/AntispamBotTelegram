<?php
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type");
header("Access-Control-Allow-Credentials: true");

include "connect.php";
$status = false;

if (!isset($_REQUEST["token"])) {
    $data = ["error" => "Некорректный запрос"];
} else {
    $token = $_REQUEST["token"];
    $stmt = $db->prepare("SELECT * FROM auth_tokens WHERE token = ?");
    if (!$stmt) die("Ошибка prepare auth_tokens");

    $stmt->bind_param("s", $token);
    $stmt->execute() or die("Не удалось обработать запрос");
    $result = $stmt->get_result();

    if ($sessionData = mysqli_fetch_array($result)) {
        $data = [];
        $adminID = $sessionData['tg_id'];

        $stmt = $db->prepare("SELECT * FROM chat_admins WHERE tg_id = ?");
        if (!$stmt) die("Ошибка prepare chat_admins");

        $stmt->bind_param("s", $adminID); // тут была ошибка: использовалась переменная $tg_id, а надо $adminID
        $stmt->execute() or die("Не удалось обработать запрос");
        $result = $stmt->get_result();

        while ($chatsIds = mysqli_fetch_array($result)) {
            $chatID = $chatsIds['chat_id'];

            $stmt = $db->prepare("SELECT * FROM chats WHERE tg_id = ?");
            if (!$stmt) die("Ошибка prepare chats");

            $stmt->bind_param("s", $chatID);
            $stmt->execute() or die("Не удалось обработать запрос");
            $chatResult = $stmt->get_result();

            if ($chatData = mysqli_fetch_array($chatResult)) {
                $chatName = $chatData['name'];
                $usersCount = $chatData['usersCount'];

                // Фильтры
                $stmt = $db->prepare("SELECT * FROM filters WHERE chatID = ?");
                if (!$stmt) die("Ошибка prepare filters");

                $stmt->bind_param("s", $chatID);
                $stmt->execute() or die("Не удалось обработать запрос");
                $filtersResult = $stmt->get_result();

                $allFilters = [];
                while ($filtersData = mysqli_fetch_array($filtersResult)) {
                    $filter = [
                        'type' => $filtersData['type'],
                        'data' => $filtersData['data']
                    ];
                    array_push($allFilters, $filter);
                }

                // Вайтлист
                $stmt = $db->prepare("SELECT * FROM whitelist WHERE chatID = ?");
                if (!$stmt) die("Ошибка prepare whitelist");

                $stmt->bind_param("s", $chatID);
                $stmt->execute() or die("Не удалось обработать запрос");
                $whitelistResult = $stmt->get_result();

                $allWhitelisted = [];
                while ($whitelistData = mysqli_fetch_array($whitelistResult)) {
                    $whitelist = [
                        'tg_id' => $whitelistData['tg_id'],
                        'date' => $whitelistData['date']
                    ];
                    array_push($allWhitelisted, $whitelist);
                }

                $fullChatData = [
                    'id' => $chatID,
                    'name' => $chatName,
                    'usersCount' => $usersCount,
                    'whitelist' => $allWhitelisted,
                    'filters' => $allFilters
                ];

                array_push($data, $fullChatData);
                $status = true;
            }
        }
    }
}

$answer = [
    "status" => $status,
    "data" => $data,
];

echo json_encode($answer, JSON_UNESCAPED_UNICODE);
?>
