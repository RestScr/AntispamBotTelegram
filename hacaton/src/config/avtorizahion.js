import {CheckSession} from "./api";

export const GetUserDataWithAuth = async () => {
    try {
        if (window.Telegram && window.Telegram.WebApp) {
            const webApp = window.Telegram.WebApp;
            const initData = window.Telegram.WebApp.initData;
            const user = webApp.initDataUnsafe?.user || {};
            try {
                const body = {
                    initData: initData,
                    tg: user.id || null
                };
                console.log(body);

                const response = await CheckSession(body);
                console.log(response);

                if (!response.ok) {
                    const data = await response.json();
                    console.log(data);
                    return data.status;

                }
            } catch (error) {
                return {error: 'Error fetching user data from the server'};
            }
        } else {
            throw new Error("Telegram WebApp API is not loaded properly.");
        }
    } catch (error) {
        console.error('Error in GetUserData:', error);
        return {error: 'Telegram WebApp API not loaded properly'};
    }
};
