import React, { useState } from "react";
import { styledComponents as S } from "./style/style";
import { useNavigate } from "react-router-dom";
import { cheackValid } from "./config/api";

export default function Valid() {
    const navigate = useNavigate();
    const [key, setKey] = useState("");
    const [status, setStatus] = useState(false);

    const handleCheckAccess = async () => {
        try {
            const res = await cheackValid({ token: key });
            const data = await res.json();
            if (data.status === true) {
                sessionStorage.setItem("Key", key);
                navigate("/channels");
            } else {
                setStatus(false);
            }
        } catch (e) {
            console.error("Ошибка запроса", e);
            setStatus(false);
        }
    };

    return (
        <S.Container>
            <h1>Название сайта</h1>
            <label>Введите ключ доступа</label>
            <input
                value={key}
                onChange={(e) => setKey(e.target.value)}
                placeholder="Ключ доступа"
            />
            <button onClick={handleCheckAccess}>Проверить</button>
            {status === true && <p>Есть доступ</p>}
            {status === false && <p>Нет доступа</p>}
        </S.Container>
    );
}
