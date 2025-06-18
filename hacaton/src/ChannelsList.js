import React, {useEffect, useState} from 'react';
import { styledComponents as S } from './style/style';
import {checkSession} from './config/api'

export default function Dashboard() {
    const [channels, setChannels] = useState([]);
    useEffect(() => {
        const loadSession = async () => {
            try {

                const body={token:"asdd"}
                const res = await checkSession(body);
                const data = await res.json();
                console.log(data);
                if (data.status === true) {

                    setChannels(data.data);
                }
            } catch (e) {
                console.error("Ошибка запроса", e);
            }
        };
        loadSession();
    },[])

    return (
        <S.Container>
            <h1>Название сайта</h1>
            <S.Container>
                <S.ButtonsInfo>
                    <S.ListCard />
                </S.ButtonsInfo>

                {channels.map((channel, index) => (
                    <S.Links to={`/channels/${channel.name}`} state={channel} key={channel.name || index}>
                        <S.ListItem>
                            <S.ListCard>{channel.name}</S.ListCard>
                        </S.ListItem>
                    </S.Links>
                ))}
            </S.Container>
        </S.Container>
    );
}
