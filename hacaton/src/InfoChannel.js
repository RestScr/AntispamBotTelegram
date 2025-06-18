import React, { useEffect, useState } from 'react';
import { Switch, Menu, Modal,Typography } from 'antd';
import { Link, useLocation } from 'react-router';
import { styledComponents as S } from './style/style';
import JoinWiteList from "./JoinWiteList";
import DeleteWhiteList from "./DeleteWhiteList";
import Log from "./Log";
import CheckUsers from "./CheckUsers";
import { buttonColor } from "./config/config";
import { loadLog, updateFilter, removeFilter } from "./config/api";

const { Text } = Typography;


export default function InfoChannel() {
    const { state } = useLocation();
    const [channel] = useState(state);

    const [filter, setFilter] = useState({
        LINK: false,
        LLM: false,
        SPAM: false,
        FLOOD: false,
    });

    // ключи, по которым сейчас идёт запрос
    const [pending, setPending] = useState(new Set());

    const [log, setLog] = useState([]);
    const [isModalOpenJoin, setIsModalOpenJoin] = useState(false);
    const [isModalOpenDelete, setIsModalOpenDelete] = useState(false);
    const [isModalOpenLog, setIsModalOpenLog] = useState(false);
    const [isModalOpenChecking, setIsModalOpenChecking] = useState(false);

    // Инициализация из channel + логи
    useEffect(() => {
        if (channel?.filters) {
            const init = { LINK:false, LLM:false, SPAM:false, FLOOD:false };
            channel.filters.forEach(k => init[k] = true);
            setFilter(init);
        }

        (async () => {
            try {
                const res = await loadLog({ chatID: channel.id });
                const data = await res.json();
                if (data.status) setLog(data.data || []);
            } catch (e) { console.error('loadLog fail', e); }
        })();
    }, [channel]);

    const toggle = async key => {
        if (pending.has(key)) return;                // защита от дабл-кликов
        const next = !filter[key];

        setPending(p => new Set(p).add(key));        // ставим лоадер

        try {
            const res = next
                ? await updateFilter({ filter: key, chatID: channel.id })
                : await removeFilter({ filter: key, chatID: channel.id });

            const data = await res.json();
            console.log(data);
            if (data.status) {                         // только если ОК
                setFilter(f => ({ ...f, [key]: next }));
            }
        } catch (e) {
            console.error('toggle fail', e);
        } finally {
            setPending(p => { const s = new Set(p); s.delete(key); return s; });
        }
    };

    return (
        <S.Container>
            <S.ButtonsInfo>
                <Link to='/'><S.Header>Название сайта</S.Header></Link>
                <S.ButtonLog danger onClick={() => setIsModalOpenLog(true)}>Логи</S.ButtonLog>
            </S.ButtonsInfo>

            <S.Container title="Фильтры">
                {['LINK','LLM','SPAM','FLOOD'].map(key => (
                    <S.FilterItem key={key}>
                        <Text style={{ marginRight: '1rem' }}>
                            {{
                                LINK: "Запрет ссылок",
                                LLM: "Детекция LLM-сообщений",
                                SPAM: "Спам-контент",
                                FLOOD: "Флуд"
                            }[key]}
                        </Text>
                        <Switch
                            checked={filter[key]}
                            loading={pending.has(key)}
                            onChange={() => toggle(key)}
                            style={{ backgroundColor: filter[key] ? buttonColor : '' }}
                        />
                    </S.FilterItem>
                ))}
            </S.Container>


            <S.Container>
                <Menu defaultSelectedKeys={['0']} mode="inline">
                    <Menu.Item key="0">Пост-модерация</Menu.Item>
                    <Menu.Item key="1">Пре-модерация</Menu.Item>
                </Menu>
            </S.Container>

            <S.ButtonsInfo>
                <S.Button danger onClick={() => setIsModalOpenJoin(true)}>Добавить в белый список</S.Button>
                <S.Button danger onClick={() => setIsModalOpenChecking(true)}>Проверка участников</S.Button>
                <S.Button danger onClick={() => setIsModalOpenDelete(true)}>Убрать из белого списка</S.Button>
            </S.ButtonsInfo>

            <Modal title="Убрать из белого списка" open={isModalOpenDelete} onCancel={() => setIsModalOpenDelete(false)} footer={null}>
                <DeleteWhiteList id={channel.id}/>
            </Modal>

            <Modal title="Добавить в белый список" open={isModalOpenJoin} onCancel={() => setIsModalOpenJoin(false)} footer={null}>
                <JoinWiteList id={channel.id}/>
            </Modal>

            <Modal title="Логи" open={isModalOpenLog} onCancel={() => setIsModalOpenLog(false)} footer={null}>
                <Log log={log}/>
            </Modal>

            <Modal title="Проверка всех пользователей" open={isModalOpenChecking} onCancel={() => setIsModalOpenChecking(false)} footer={null}>
                <CheckUsers/>
            </Modal>
        </S.Container>
    );
}
