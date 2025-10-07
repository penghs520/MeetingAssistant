/**
 * Meeting Assistant App
 * AI-powered meeting assistant with real-time transcription
 *
 * @format
 */

import React from 'react';
import { StatusBar } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import MeetingListScreen from './src/screens/MeetingListScreen';
import MeetingScreen from './src/screens/MeetingScreen';
import MeetingDetailScreen from './src/screens/MeetingDetailScreen';

const Stack = createNativeStackNavigator();

function App() {
  return (
    <SafeAreaProvider>
      <StatusBar barStyle="light-content" backgroundColor="#4CAF50" />
      <NavigationContainer>
        <Stack.Navigator
          initialRouteName="MeetingList"
          screenOptions={{
            headerShown: false,
          }}
        >
          <Stack.Screen name="MeetingList" component={MeetingListScreen} />
          <Stack.Screen name="Meeting" component={MeetingScreen} />
          <Stack.Screen name="MeetingDetail" component={MeetingDetailScreen} />
        </Stack.Navigator>
      </NavigationContainer>
    </SafeAreaProvider>
  );
}

export default App;
